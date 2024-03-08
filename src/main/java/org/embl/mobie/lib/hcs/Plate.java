/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.hcs;

import bdv.viewer.Source;
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import mpicbg.spim.data.generic.base.Entity;
import org.embl.mobie.DataStore;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.toml.TPosition;
import org.embl.mobie.io.toml.ZPosition;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.color.ColorHelper;

import ch.epfl.biop.bdv.img.bioformats.entity.SeriesIndex;
import org.embl.mobie.lib.hcs.omezarr.OMEZarrHCSHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Plate
{
	private final String hcsDirectory;
	private HCSPattern hcsPattern;
	private HashMap< Channel, Map< Well, Set< Site > > > channelWellSites;
	private double[] siteRealDimensions;
	private int sitesPerWell;
	private int[] siteDimensions;
	private VoxelDimensions voxelDimensions;
	private Set< TPosition > tPositions;
	private Set< ZPosition > zPositions;
	private int wellsPerPlate;
	private ImageDataFormat imageDataFormat;
	private OperettaMetadata operettaMetadata;
	private AbstractSpimData< ? > spimDataPlate;
	private boolean siteIDsAreOneBased = true;
	private boolean is2d = true;
	private int numSlices;


	public Plate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		// FIXME: fetch operetta paths from XML?!
		// FIXME: fetch OME-Zarr paths entry point JSON?!

		IJ.log( "Looking for image files..." );
		long start = System.currentTimeMillis();
		List< String > imagePaths;

		if ( hcsDirectory.endsWith( ".zarr" ) )
		{
			hcsPattern = HCSPattern.OMEZarr;

			if ( IOHelper.getType( hcsDirectory ).equals( IOHelper.ResourceType.S3 ) )
			{
				imageDataFormat = ImageDataFormat.OmeZarrS3;
				ThreadHelper.setNumIoThreads( Math.max( 16, ThreadHelper.getNumIoThreads() ) );
			}
			else
			{
				imageDataFormat = ImageDataFormat.OmeZarr;
			}

			imagePaths = OMEZarrHCSHelper.sitePathsFromMetadata( hcsDirectory );

			final String firstImagePath = imagePaths.get( 0 );
			ImageData< ? > imageData = ImageDataOpener.open( imagePaths.get( 0 ), imageDataFormat, ThreadHelper.sharedQueue );
			int numChannels = imageData.getNumDatasets();

			List< String > channelNames = IntStream.range( 0, numChannels )
					.mapToObj( channelIndex -> imageData.getSourcePair( channelIndex ).getA().getName() )
					.collect( Collectors.toList() );
			hcsPattern.setChannelNames( channelNames );
		}
		else
		{
			if( IOHelper.getType( hcsDirectory ).equals( IOHelper.ResourceType.S3 ) )
			{
				imageDataFormat = ImageDataFormat.BioFormatsS3;
				imagePaths = S3Utils.getS3FilePaths( hcsDirectory );
				ThreadHelper.setNumIoThreads( Math.max( 16, ThreadHelper.getNumIoThreads() ) );
			}
			else
			{
				imageDataFormat = ImageDataFormat.BioFormats;
				imagePaths = Files.walk( Paths.get( hcsDirectory ), 3 )
						.map( p -> p.toString() )
						.collect( Collectors.toList() );
			}

			hcsPattern = determineHCSPattern( hcsDirectory, imagePaths );
			imagePaths = imagePaths.stream()
					.filter( path -> hcsPattern.setMatcher( path ) ) // skip files like .DS_Store a.s.o.
					.collect( Collectors.toList() );

			if ( hcsPattern.equals( HCSPattern.Operetta ) )
			{
				// only keep paths that are also in the XML
				//final File xml = new File( hcsDirectory, "Index.idx.xml" );
				final File xml = new File( hcsDirectory, "Index.xml" );
				operettaMetadata = new OperettaMetadata( xml );
				imagePaths = imagePaths.stream()
						.filter( path -> operettaMetadata.contains( path ) ) // skip files like .DS_Store a.s.o.
						.collect( Collectors.toList() );
			}
			else if ( hcsPattern.equals( HCSPattern.YokogawaCQ1 ) )
			{
				imageDataFormat = ImageDataFormat.Tiff;
			}
		}
		IJ.log( "Found " + imagePaths.size() + " image files in " + ( System.currentTimeMillis() - start ) + " ms." );
		IJ.log( "HCS pattern: " + getHcsPattern() );
		IJ.log( "Image data format: " + imageDataFormat );

		buildPlateMap( imagePaths );
	}

	private void buildPlateMap( List< String > imagePaths )
	{
		channelWellSites = new HashMap<>();
		tPositions = new HashSet<>();
		zPositions = new HashSet<>();

		IJ.log("Parsing metadata...");

		for ( String imagePath : imagePaths )
		{
			hcsPattern.setMatcher( imagePath );

			// some formats contain multiple channels in one file
			List< String > channelNames = hcsPattern.getChannels();

			for ( String channelName : channelNames )
			{
				Channel channel = getChannel( channelWellSites, channelName );

				if ( channel == null )
				{
					// configure channel properties
					//
					channel = new Channel( channelName, channelNames.indexOf( channelName ) );
					channelWellSites.put( channel, new HashMap<>() );


					// Open for metadata only
					ImageData< ? > imageData = ImageDataOpener.open( imagePath, imageDataFormat, ThreadHelper.sharedQueue );

					// set channel metadata
					//
					if ( operettaMetadata != null ) // Do we still want to support the operetta stuff at all?
					{
						final String color = operettaMetadata.getColor( imagePath );
						channel.setColor( color );

						// TODO: There does not always seem to be enough metadata for the
						//   contrast limits, thus opening one image may be worth it
						//   then convert to imagePlus and run once auto contrast on it
						final double[] contrastLimits = operettaMetadata.getContrastLimits( imagePath );
						channel.setContrastLimits( contrastLimits );
					}
					else // from image file
					{
						int datasetIndex = channel.getIndex();

						IJ.log( "Fetching metadata for setup " + channelName + " from " + imagePath );
						numSlices = ( int ) imageData.getSourcePair( datasetIndex ).getB().getSource( 0, 0 ).dimension( 2 );

						channel.setColor( ColorHelper.getString( imageData.getMetadata( datasetIndex ).getColor() ) );

						channel.setContrastLimits( new double[]{
								imageData.getMetadata( datasetIndex ).minIntensity(),
								imageData.getMetadata( datasetIndex ).maxIntensity()
						} );
					}

					// determine spatial metadata (for all channels the same)
					//
					if ( voxelDimensions == null )
					{
						if ( operettaMetadata != null )
						{
							voxelDimensions = operettaMetadata.getVoxelDimensions( imagePath );
							siteDimensions = operettaMetadata.getSiteDimensions( imagePath );
						}
						else // from image file
						{
							Source< ? > source = imageData.getSourcePair( channel.getIndex() ).getA();

							voxelDimensions = source.getVoxelDimensions();

							if ( hcsPattern.hasZ() )
							{
								/*
								If the z-positions are distributed over multiple files
								typically the z-calibration metadata in the individual files is wrong.
								We thus just put something sensible here such that browsing in BDV along the
								z-axis is convenient
								 */
								voxelDimensions = new FinalVoxelDimensions(
										voxelDimensions.unit(),
										voxelDimensions.dimension( 0 ),
										voxelDimensions.dimension( 1 ),
										10 * voxelDimensions.dimension( 1 )
								);
							}

							long width = source.getSource( 0, 0 ).dimension( 0 );
							long height = source.getSource( 0, 0 ).dimension( 1 );
							siteDimensions = new int[]{ ( int ) width, ( int ) height };
						}

						// compute derived spatial metadata
						//
						siteRealDimensions = new double[]{
								siteDimensions[ 0 ] * voxelDimensions.dimension( 0 ),
								siteDimensions[ 1 ] * voxelDimensions.dimension( 1 ) };

						siteRealDimensions = new double[]{
								siteDimensions[ 0 ] * voxelDimensions.dimension( 0 ),
								siteDimensions[ 1 ] * voxelDimensions.dimension( 1 ) };
					}
				}

				// well
				//
				String wellGroup = hcsPattern.getWellGroup();
				Well well = getWell( channelWellSites, channel, wellGroup );
				if ( well == null )
				{
					well = new Well( wellGroup );
					channelWellSites.get( channel ).put( well, new HashSet<>() );
					final int numWells = channelWellSites.get( channel ).size();
					if ( numWells > wellsPerPlate )
						wellsPerPlate = numWells;
				}

				// site
				//
				final String siteGroup = hcsPattern.getSiteGroup();
				Site site = getSite( channelWellSites, channel, well, siteGroup );
				if ( site == null )
				{
					if ( imageDataFormat.equals( ImageDataFormat.SpimData ) )
					{
						final int imageIndex = operettaMetadata.getImageIndex( imagePath );
						final BasicViewSetup viewSetup = spimDataPlate.getSequenceDescription().getViewSetupsOrdered().get( imageIndex );
						IJ.log( "" );
						final Map< String, Entity > attributes = viewSetup.getAttributes();
						IJ.log( "Image index:" + imageIndex );
						IJ.log( "Series index: " + ( ( SeriesIndex ) attributes.get( "seriesindex" ) ).getId() );
						IJ.log( "Setup name: " + viewSetup.getName() );
						IJ.log( "File name: " + new File( imagePath ).getName() );
						site = new Site( siteGroup, imageDataFormat, spimDataPlate, imageIndex );
					}
					else
					{
						site = new Site( siteGroup, imageDataFormat );
					}
					site.setDimensions( siteDimensions );
					site.setVoxelDimensions( voxelDimensions );
					channelWellSites.get( channel ).get( well ).add( site );
					if ( Integer.parseInt( site.getId() ) == 0 )
						siteIDsAreOneBased = false; // zero based
					final int numSites = channelWellSites.get( channel ).get( well ).size();
					if ( numSites > sitesPerWell )
						sitesPerWell = numSites; // needed to compute the site position within a well
				}

				if ( hcsPattern.equals( HCSPattern.OMEZarr ) )
				{
					site.absolutePath = imagePath;
					site.channel = channel.getIndex();
				}
				else
				{
					final String t = hcsPattern.getT();
					final String z = hcsPattern.getZ();
					site.addPath( t, z, imagePath );
					tPositions.add( new TPosition( t ) );
					numSlices = Math.max( numSlices, site.getZPositions().size() );
				}
			}
		}

		IJ.log( "Initialised HCS plate: " + getName() );
		IJ.log( "Wells: " + wellsPerPlate );
		IJ.log( "Sites per well: " + sitesPerWell );
		IJ.log( "Channels: " + channelWellSites.keySet().size() );
		IJ.log( "Frames: " + tPositions.size() );
		IJ.log( "Slices: " + numSlices ); // one file per slice, many slices in one file

		if ( numSlices > 1 )
			is2d = false;
	}


	private Channel getChannel( HashMap< Channel, Map< Well, Set< Site > > > channelWellSites, String channelName )
	{
		try
		{
			return channelWellSites.keySet().stream().filter( c -> c.getName().equals( channelName ) ).findFirst().get();
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}

	}

	private Well getWell( HashMap< Channel, Map< Well, Set< Site > > > channelWellSites, Channel channel, String wellName )
	{
		try
		{
			return channelWellSites.get( channel ).keySet().stream().filter( w -> w.getName().equals( wellName ) ).findFirst().get();
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}
	}

	private Site getSite( HashMap< Channel, Map< Well, Set< Site > > > channelWellSites, Channel channel, Well well, String siteName )
	{
		try
		{
			return channelWellSites.get( channel ).get( well ).stream().filter( s -> s.getId().equals( siteName ) ).findFirst().get();
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}
	}

	private HCSPattern determineHCSPattern( String hcsDirectory, List< String > paths )
	{
		for ( String path : paths )
		{
			final HCSPattern hcsPattern = HCSPattern.fromPath( path );
			if ( hcsPattern != null )
			{
				return hcsPattern;
			}
		}

		throw new RuntimeException( "Could not determine HCSPattern for " + hcsDirectory );
	}

	public Set< Channel > getChannels()
	{
		return channelWellSites.keySet();
	}

	public Set< Well > getWells( Channel channel )
	{
		return channelWellSites.get( channel ).keySet();
	}

	public Set< Site > getSites( Channel channel, Well well )
	{
		return channelWellSites.get( channel ).get( well );
	}

	public int[] getGridPosition( Site site )
	{
		switch ( hcsPattern )
		{
			case Operetta:
				return getOperettaGridPosition( site );
			case MolecularDevices:
				return getMolecularDevicesGridPosition( site );
			default:
				return getDefaultGridPosition( site );
		}
	}

	public int[] getMolecularDevicesGridPosition( Site site )
	{
		if ( sitesPerWell == 1 )
			return new int[]{ 0, 0 };

		int siteIndex = Integer.parseInt( site.getId() ) - 1;
		int numColumns = (int) Math.ceil( Math.sqrt( sitesPerWell ) );

		int[] gridPosition = new int[ 2 ];
		gridPosition[ 0 ] = siteIndex % numColumns; // column
		gridPosition[ 1 ] = siteIndex / numColumns; // row

		return gridPosition;
	}

	private int[] getOperettaGridPosition( Site site )
	{
		final String path = site.getPaths().values().iterator().next().values().iterator().next();
		final double[] realPosition = operettaMetadata.getRealPosition( path );

		final int[] position = new int[ 2 ];
		for ( int d = 0; d < 2; d++ )
		{
			position[ d ] = (int) ( realPosition[ d ] / siteRealDimensions [ d ] );
		}

		return position;
	}

	public int[] getDefaultGridPosition( Site site )
	{
		if ( sitesPerWell == 1 )
			return new int[]{ 0, 0 };

		// TODO: not obvious that the ID can be parsed to an Integer here
		int siteIndex = Integer.parseInt( site.getId() );
		if ( siteIDsAreOneBased )
			siteIndex -= 1;
		int numSiteColumns = (int) Math.sqrt( sitesPerWell );

		int[] gridPosition = new int[ 2 ];
		gridPosition[ 0 ] = siteIndex % numSiteColumns; // column
		gridPosition[ 1 ] = siteIndex / numSiteColumns; // row

		return gridPosition;
	}

	public int[] getWellGridPosition( Well well )
	{
		return hcsPattern.decodeWellGridPosition( well.getName() );
	}

	public boolean is2D()
	{
		return is2d;
	}

	public String getName()
	{
		return new File( hcsDirectory ).getName();
	}

	public HCSPattern getHcsPattern()
	{
		return hcsPattern;
	}

	public double[] getSiteRealDimensions()
	{
		return siteRealDimensions;
	}

	public int[] getSiteDimensions()
	{
		return siteDimensions;
	}

	public Set< TPosition > getTPositions()
	{
		return tPositions;
	}

	public int getSitesPerWell()
	{
		return sitesPerWell;
	}
}
