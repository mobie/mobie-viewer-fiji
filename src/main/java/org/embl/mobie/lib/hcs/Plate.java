package org.embl.mobie.lib.hcs;

import bdv.SpimSource;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.img.bioformats.entity.SeriesIndex;
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.measure.Calibration;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.source.SourceToImagePlusConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
	private int wellsPerPlate;
	private ImageDataFormat imageDataFormat;
	private OperettaMetadata metadata;
	private AbstractSpimData< ? > spimData;

	public Plate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		final List< String > paths = Files.walk( Paths.get( hcsDirectory ) ).map( p -> p.toString() ).collect( Collectors.toList() );

		hcsPattern = determineHCSPattern( hcsDirectory, paths );

		if ( hcsPattern == HCSPattern.Operetta )
		{
			final Optional< String > xml = paths.stream().filter( path -> path.endsWith( ".xml" ) ).findFirst();
			if ( ! xml.isPresent() )
				throw new RuntimeException("XML file is missing; cannot open Operetta files without the XML.");
			IJ.log( "Parsing XML: " + xml.get() + " ...");
			imageDataFormat = ImageDataFormat.SpimData;
			spimData = new SpimDataOpener().openWithBioFormats( xml.get(), ThreadHelper.sharedQueue );
			metadata = new OperettaMetadata( new File( xml.get() ) );
			IJ.log( "Images in XML: " + spimData.getSequenceDescription().getViewSetupsOrdered().size() );
		}

		buildPlateMap( paths );
	}

	private void buildPlateMap( List< String > paths )
	{
		channelWellSites = new HashMap<>();
		tPositions = new HashSet<>();
		int numValidImages = 0;

		IJ.log("Files: " + paths.size() );

		for ( String path : paths )
		{
			if ( ! hcsPattern.setPath( path ) )
				continue;

			if ( metadata != null )
			{
				if ( ! metadata.contains( path ) )
				{
					IJ.log( "[WARNING] No metadata found for " + path );
					continue;
				}
			}

			numValidImages++;

			if ( imageDataFormat == null )
			{
				imageDataFormat = ImageDataFormat.fromPath( path );
				IJ.log( "Image data format: " + imageDataFormat.toString() );
			}


			//System.out.println( path );

			// store this file's channel, well, and site
			//

			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/972
			//    WellH06_PointH06_0007_ChannelDAPI,WF_GFP,TRITC,WF_Cy5,DIA_Seq0502.tiff
			//    Maybe change to hcsPattern.getChannels(); (plural) ?
			//    and then loop through the channels

			// channel
			//
			String channelGroup = hcsPattern.getChannelGroup();
			Channel channel = getChannel( channelWellSites, channelGroup );

			if ( channel == null )
			{
				// configure channel properties
				//
				channel = new Channel( channelGroup );
				channelWellSites.put( channel, new HashMap<>() );

				ImagePlus imagePlus = metadata == null ? openImagePlus( path ) : null;

				// set channel metadata
				//
				if ( metadata != null )
				{
					final String color = metadata.getColor( path );
					channel.setColor( color );

					// TODO: There does not always seem to be enough metadata for the
					//   contrast limits, thus opening one image may be worth it
					final double[] contrastLimits = metadata.getContrastLimits( path );
					channel.setContrastLimits( contrastLimits );
				}
				else // from image file
				{
					final String color = ColorHelper.getString( imagePlus.getLuts()[ 0 ] );
					channel.setColor( color );

					final double[] contrastLimits = new double[]
					{
							imagePlus.getDisplayRangeMin(),
							imagePlus.getDisplayRangeMax()
					};
					channel.setContrastLimits( contrastLimits );
				}

				// determine spatial metadata (for all channels the same)
				//
				if ( voxelDimensions == null )
				{
					if ( metadata != null )
					{
						voxelDimensions = metadata.getVoxelDimensions( path );
						siteDimensions = metadata.getSiteDimensions( path );
					}
					else // from image file
					{
						final Calibration calibration = imagePlus.getCalibration();
						voxelDimensions = new FinalVoxelDimensions( calibration.getUnit(), calibration.pixelWidth, calibration.pixelHeight, calibration.pixelDepth );

						siteDimensions = new int[]{ imagePlus.getWidth(), imagePlus.getHeight() };
					}

					// compute derived spatial metadata
					//
					siteRealDimensions = new double[]
					{
						siteDimensions[ 0 ] * voxelDimensions.dimension( 0 ),
						siteDimensions[ 1 ] * voxelDimensions.dimension( 1 )
					};

					siteRealDimensions = new double[]
					{
						siteDimensions[ 0 ] * voxelDimensions.dimension( 0 ),
						siteDimensions[ 1 ] * voxelDimensions.dimension( 1 )
					};
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
					final int imageIndex = metadata.getImageIndex( path );
//					final BasicViewSetup viewSetup = spimData.getSequenceDescription().getViewSetupsOrdered().get( imageIndex );
//					IJ.log("");
//					final Map< String, Entity > attributes = viewSetup.getAttributes();
//					IJ.log( "Image index:" + imageIndex );
//					IJ.log( "Series index: " + (( SeriesIndex ) attributes.get( "seriesindex" )).getId() );
//					IJ.log( "Setup name: " + viewSetup.getName() );
//					IJ.log( "File name: " + new File( path ).getName() );
					site = new Site( siteGroup, imageDataFormat, spimData, imageIndex );
				}
				else
				{
					site = new Site( siteGroup, imageDataFormat );
				}
				site.setDimensions( siteDimensions );
				site.setVoxelDimensions( voxelDimensions );
				channelWellSites.get( channel ).get( well ).add( site );
				final int numSites = channelWellSites.get( channel ).get( well ).size();
				if ( numSites > sitesPerWell )
					sitesPerWell = numSites; // needed to compute the site position within a well
			}

			final String t = hcsPattern.getT();
			final String z = hcsPattern.getZ();
			site.addPath( t, z, path );

			tPositions.add( new TPosition( t ) );
		}

		IJ.log( "Initialised HCS plate: " + getName() );
		IJ.log( "Images: " + numValidImages );
		IJ.log( "Channels: " + channelWellSites.keySet().size() );
		IJ.log( "Wells: " + wellsPerPlate );
		IJ.log( "Sites per well: " + sitesPerWell );
	}

	private ImagePlus openImagePlus( String path )
	{
		if ( spimData != null )
		{
			final int imageIndex = metadata.getImageIndex( path );
			final Source< ? > source =  new SpimSource<>( spimData, imageIndex, "" );
			final ImagePlus imagePlus = new SourceToImagePlusConverter( source ).getImagePlus( 0 );
			return imagePlus;
		}

		if ( imageDataFormat.equals( ImageDataFormat.Tiff ) )
		{
			final File file = new File( path );
			return ( new Opener() ).openTiff( file.getParent(), file.getName() );
		}
		else
		{
			return IJ.openImage( path );
		}
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
			return channelWellSites.get( channel ).get( well ).stream().filter( s -> s.getName().equals( siteName ) ).findFirst().get();
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
				IJ.log( "HCS Pattern: " + hcsPattern );
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

		int siteIndex = Integer.parseInt( site.getName() ) - 1;
		int numColumns = (int) Math.ceil( Math.sqrt( sitesPerWell ) );

		int[] gridPosition = new int[ 2 ];
		gridPosition[ 0 ] = siteIndex % numColumns; // column
		gridPosition[ 1 ] = siteIndex / numColumns; // row

		return gridPosition;
	}

	private int[] getOperettaGridPosition( Site site )
	{
		final String path = site.getPaths().values().iterator().next().values().iterator().next();
		final double[] realPosition = metadata.getRealPosition( path );

		final int[] position = new int[ 2 ];
		for ( int d = 0; d < 2; d++ )
		{
			position[ d ] = (int) Math.round( realPosition[ d ] / siteRealDimensions [ d ]  );
		}

		return position;
	}

	public int[] getDefaultGridPosition( Site site )
	{
		if ( sitesPerWell == 1 )
			return new int[]{ 0, 0 };

		int siteIndex = Integer.parseInt( site.getName() ) - 1;
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
		return true; // for now...
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
