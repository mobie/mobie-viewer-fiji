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
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.source.SourceToImagePlusConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private OperettaMetadata hcsMetadata;
	private AbstractSpimData< ? > spimDataPlate;

	public Plate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		final List< String > paths;
		if ( hcsDirectory.endsWith( ".zarr" ) )
		{
			final int minDepth = 3;
			final int maxDepth = 3;
			final Path rootPath = Paths.get(hcsDirectory);
			final int rootPathDepth = rootPath.getNameCount();
			paths = Files.walk( rootPath, maxDepth )
					.filter( e -> e.toFile().isDirectory() )
					.filter( e -> e.getNameCount() - rootPathDepth >= minDepth )
					.map( e -> e.toString() )
					.collect( Collectors.toList() );
		}
		else
		{
			paths = Files.walk( Paths.get( hcsDirectory ), 3 ).map( p -> p.toString() ).collect( Collectors.toList() );
		}

		hcsPattern = determineHCSPattern( hcsDirectory, paths );

		if ( hcsPattern == HCSPattern.Operetta )
		{
			final Optional< String > xml = paths.stream().filter( path -> path.endsWith( ".xml" ) ).findFirst();
			if ( ! xml.isPresent() )
				throw new RuntimeException("XML file is missing; cannot open Operetta files without the XML.");
			IJ.log( "Parsing XML: " + xml.get() + " ...");
			imageDataFormat = ImageDataFormat.SpimData;
			spimDataPlate = new SpimDataOpener().openWithBioFormats( xml.get(), ThreadHelper.sharedQueue );
			hcsMetadata = new OperettaMetadata( new File( xml.get() ) );
			IJ.log( "Images in XML: " + spimDataPlate.getSequenceDescription().getViewSetupsOrdered().size() );
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

			if ( hcsMetadata != null )
			{
				if ( ! hcsMetadata.contains( path ) )
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

			// store this file's channel(s), well, and site
			//

			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/972
			// for some formats one file can contain multiple channels,
			// thus a list is returned.
			List< String > channelNames = hcsPattern.getChannels();
			for ( String channelName : channelNames )
			{
				Channel channel = getChannel( channelWellSites, channelName );

				if ( channel == null )
				{
					// configure channel properties
					//
					channel = new Channel( channelName );
					channelWellSites.put( channel, new HashMap<>() );

					ImagePlus singleChannel = hcsMetadata == null ? openImagePlus( path, channelName ) : null;

					// set channel metadata
					//
					if ( hcsMetadata != null )
					{
						final String color = hcsMetadata.getColor( path );
						channel.setColor( color );

						// TODO: There does not always seem to be enough metadata for the
						//   contrast limits, thus opening one image may be worth it
						final double[] contrastLimits = hcsMetadata.getContrastLimits( path );
						channel.setContrastLimits( contrastLimits );
					} else // from image file
					{
						final String color = ColorHelper.getString( singleChannel.getLuts()[ 0 ] );
						channel.setColor( color );

						final double[] contrastLimits = new double[]{
								singleChannel.getDisplayRangeMin(),
								singleChannel.getDisplayRangeMax()
						};
						channel.setContrastLimits( contrastLimits );
					}

					// determine spatial metadata (for all channels the same)
					//
					if ( voxelDimensions == null )
					{
						if ( hcsMetadata != null )
						{
							voxelDimensions = hcsMetadata.getVoxelDimensions( path );
							siteDimensions = hcsMetadata.getSiteDimensions( path );
						} else // from image file
						{
							final Calibration calibration = singleChannel.getCalibration();
							voxelDimensions = new FinalVoxelDimensions( calibration.getUnit(), calibration.pixelWidth, calibration.pixelHeight, calibration.pixelDepth );
							siteDimensions = new int[]{ singleChannel.getWidth(), singleChannel.getHeight() };
						}
						final String color = ColorHelper.getString( singleChannel.getLuts()[ 0 ] );
						channel.setColor( color );
						final double[] contrastLimits = new double[]{
								singleChannel.getDisplayRangeMin(),
								singleChannel.getDisplayRangeMax() };
						channel.setContrastLimits( contrastLimits );

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
				String wellGroup = hcsPattern.getWell();
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
				final String siteGroup = hcsPattern.getSite();
				Site site = getSite( channelWellSites, channel, well, siteGroup );
				if ( site == null )
				{
					if ( imageDataFormat.equals( ImageDataFormat.SpimData ) )
					{
						final int imageIndex = hcsMetadata.getImageIndex( path );
						final BasicViewSetup viewSetup = spimDataPlate.getSequenceDescription().getViewSetupsOrdered().get( imageIndex );
						IJ.log( "" );
						final Map< String, Entity > attributes = viewSetup.getAttributes();
						IJ.log( "Image index:" + imageIndex );
						IJ.log( "Series index: " + ( ( SeriesIndex ) attributes.get( "seriesindex" ) ).getId() );
						IJ.log( "Setup name: " + viewSetup.getName() );
						IJ.log( "File name: " + new File( path ).getName() );
						site = new Site( siteGroup, imageDataFormat, spimDataPlate, imageIndex );
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

				if ( hcsPattern.equals( HCSPattern.OMEZarr ) )
				{
					site.absolutePath = path;
					tPositions.add( new TPosition( "0" ) ); // TODO: add all time points
				}
				else
				{
					final String t = hcsPattern.getT();
					final String z = hcsPattern.getZ();
					site.addPath( t, z, path );
					tPositions.add( new TPosition( t ) );
				}
			}
		}

		IJ.log( "Initialised HCS plate: " + getName() );
		IJ.log( "Images: " + numValidImages );
		IJ.log( "Channels: " + channelWellSites.keySet().size() );
		IJ.log( "Wells: " + wellsPerPlate );
		IJ.log( "Sites per well: " + sitesPerWell );
	}

	private ImagePlus openImagePlus( String path, String channelName )
	{
		if ( spimDataPlate != null )
		{
			final int imageIndex = hcsMetadata.getImageIndex( path );
			final Source< ? > source =  new SpimSource<>( spimDataPlate, imageIndex, "" );
			final ImagePlus imagePlus = new SourceToImagePlusConverter( source ).getImagePlus( 0 );
			return imagePlus;
		}

		if ( imageDataFormat.equals( ImageDataFormat.Tiff ) )
		{
			final File file = new File( path );
			return ( new Opener() ).openTiff( file.getParent(), file.getName() );
		}

		if ( imageDataFormat.equals( ImageDataFormat.OmeZarr ) )
		{
			final int setupID = Integer.parseInt( channelName );
			return MoBIEHelper.openOMEZarrAsImagePLus( path, setupID );
		}

		return IJ.openImage( path );

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
			return channelWellSites.get( channel ).get( well ).stream().filter( s -> s.getID().equals( siteName ) ).findFirst().get();
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

		int siteIndex = Integer.parseInt( site.getID() ) - 1;
		int numColumns = (int) Math.ceil( Math.sqrt( sitesPerWell ) );

		int[] gridPosition = new int[ 2 ];
		gridPosition[ 0 ] = siteIndex % numColumns; // column
		gridPosition[ 1 ] = siteIndex / numColumns; // row

		return gridPosition;
	}

	private int[] getOperettaGridPosition( Site site )
	{
		final String path = site.getPaths().values().iterator().next().values().iterator().next();
		final double[] realPosition = hcsMetadata.getRealPosition( path );

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

		int siteIndex = Integer.parseInt( site.getID() ) - 1;
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
