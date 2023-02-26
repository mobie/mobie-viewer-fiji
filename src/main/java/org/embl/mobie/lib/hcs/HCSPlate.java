package org.embl.mobie.lib.hcs;

import ij.IJ;
import ij.ImagePlus;
import org.embl.mobie.lib.color.ColorHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HCSPlate
{
	private final String hcsDirectory;
	private HCSPattern hcsPattern;
	private HashMap< String, Map< String, Set< String > > > channelWellSites;

	// TODO replace String with StorageLocation ?!
	private HashMap< String, Map< String, Map< String, String > > > siteTZPath;

	private boolean isTimelapse = false;
	private boolean isVolume = false;

	public HCSPlate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		final List< String > paths = Files.walk( Paths.get( hcsDirectory ) ).map( p -> p.toString() ).collect( Collectors.toList() );

		hcsPattern = determineHCSPattern( hcsDirectory, paths );

		buildPlateMap( paths );
	}

	private void buildPlateMap( List< String > paths )
	{
		channelWellSites = new HashMap<>();
		siteTZPath = new HashMap<>();

		for ( String path : paths )
		{
			if ( ! hcsPattern.setPath( path ) )
				continue;

			// store this file's channel, well, and site
			//

			String channel = hcsPattern.getChannel();
			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/972
			//    WellH06_PointH06_0007_ChannelDAPI,WF_GFP,TRITC,WF_Cy5,DIA_Seq0502.tiff
			//    Maybe change to hcsPattern.getChannels(); (plural) ?
			//    and then loop through the channels
			//    and add a suffix to the path "--c0" to indicate which channel to load?
			//    Maybe I need something more complex than a String to represent the path?
			//    In fact, could I use StorageLocation already here?

			if ( ! channelWellSites.containsKey( channel ) )
			{
				final HashMap< String, Set< String > > wells = new HashMap<>();
				channelWellSites.put( channel, wells );
			}

			String well = hcsPattern.getWell();
			if ( ! channelWellSites.get( channel ).containsKey( well ) )
			{
				final HashSet< String > sites = new HashSet<>();
				channelWellSites.get( channel ).put( well, sites );
			}

			final String site = hcsPattern.getSite();
			channelWellSites.get( channel ).get( well ).add( site );

			// store this site's timepoint, z-plane, and path
			//

			final String siteKey = getSiteKey( channel, well, site );
			if ( ! siteTZPath.containsKey( siteKey ) )
			{
				final Map< String, Map< String, String > > tzpath = new LinkedHashMap();
				siteTZPath.put( siteKey, tzpath );
			}

			final String t = hcsPattern.getT();
			if ( ! siteTZPath.get( siteKey ).containsKey( t ) )
			{
				final Map< String, String > zpath = new LinkedHashMap();
				siteTZPath.get( siteKey ).put( t, zpath );
			}

			final String z = hcsPattern.getZ();
			siteTZPath.get( siteKey ).get( t ).put( z, path );

			if ( siteTZPath.get( siteKey).size() > 1 )
			{
				isTimelapse = true;
			}

			if ( siteTZPath.get( siteKey).get( t ).size() > 1 )
			{
				isVolume = true;
			}
		}
	}

	public String getSiteKey( String channel, String well, String site )
	{
		return channel + "-" + well + "-" + site;
	}

	private HCSPattern determineHCSPattern( String hcsDirectory, List< String > paths )
	{
		for ( String path : paths )
		{
			final HCSPattern hcsPattern = HCSPattern.fromPath( path );
			if ( hcsPattern != null )
				return hcsPattern;
		}

		throw new RuntimeException( "Could not determine HCSPattern for " + hcsDirectory );
	}

	public Set< String > getChannels()
	{
		return channelWellSites.keySet();
	}

	public Set< String > getWells( String channel )
	{
		return channelWellSites.get( channel ).keySet();
	}

	public Set< String > getSites( String channel, String well )
	{
		return channelWellSites.get( channel ).get( well );
	}

	public Map< String, Map< String, String > > getTZPaths( String channel, String well, String site )
	{
		return siteTZPath.get( getSiteKey( channel, well, site ) );
	}

	public String getPath( String channel, String well, String site )
	{
		return getFirstPath( siteTZPath.get( getSiteKey( channel, well, site ) ) );
	}

	public int[] getSiteGridPosition( String channel, String well, String site )
	{
		switch ( hcsPattern )
		{
			default:
			case Operetta:
				int numSites = channelWellSites.get( channel ).get( well ).size();
				int siteIndex = Integer.parseInt( site ) - 1;
				int numSiteColumns = (int) Math.sqrt( numSites );

				int[] gridPosition = new int[ 2 ];
				gridPosition[ 0 ] = siteIndex % numSiteColumns; // column
				gridPosition[ 1 ] = siteIndex / numSiteColumns; // row

				// System.out.println( "Site  = " + site + ", c = " + gridPosition[ 0 ] + ", r = " + gridPosition[ 1 ]);

				return gridPosition;
		}
	}

	public int[] getWellGridPosition( String well )
	{
		switch ( hcsPattern )
		{
			default:
			case Operetta:
				final int[] gridPosition = hcsPattern.getWellGridPosition( well );
				// System.out.println( "Well  = " + well + ", c = " + gridPosition[ 0 ] + ", r = " + gridPosition[ 1 ]);
				return gridPosition;
		}
	}

	public double[] getContrastLimits( String channel )
	{
		final String sitePath = getFirstSitePath( channel );
		final ImagePlus imagePlus = IJ.openImage( sitePath );
		final double[] contrastLimits = new double[ 2 ];
		contrastLimits[ 0 ] = imagePlus.getDisplayRangeMin();
		contrastLimits[ 1 ] = imagePlus.getDisplayRangeMax();
		return contrastLimits;
	}

	public String getColor( String channel )
	{
		final String path = getFirstSitePath( channel );
		final ImagePlus imagePlus = IJ.openImage( path );
		return ColorHelper.getString( imagePlus.getLuts()[ 0 ] );
	}

	public double[] getSiteRealDimensions( String channel )
	{
		final String path = getFirstSitePath( channel );
		final ImagePlus imagePlus = IJ.openImage( path );
		final double[] realDimensions = new double[ 2 ];
		realDimensions[ 0 ] = imagePlus.getWidth() * imagePlus.getCalibration().pixelWidth;
		realDimensions[ 1 ] = imagePlus.getHeight() * imagePlus.getCalibration().pixelHeight;
		return realDimensions;
	}

	public boolean is2D()
	{
		final String path = getFirstSitePath( getChannels().iterator().next() );
		final ImagePlus imagePlus = IJ.openImage( path );
		return imagePlus.getDimensions()[ 3 ] == 1;
	}

	private String getFirstSitePath( String channel )
	{
		final String firstWell = channelWellSites.get( channel ).keySet().iterator().next();
		final String firstSite = channelWellSites.get( channel ).get( firstWell ).iterator().next();
		final Map< String, Map< String, String > > tzPaths = getTZPaths( channel, firstWell, firstSite );

		return getFirstPath( tzPaths );
	}

	public static String getFirstPath( Map< String, Map< String, String > > tzPaths )
	{
		// first timepoint
		final Map< String, String > zPath = tzPaths.values().iterator().next();
		// first z-plane
		final String path = zPath.values().iterator().next();

		return path;
	}

	public static int[] getWellLayout( int numWells )
	{
		int[] wellDimensions = new int[ 2 ];

		if ( numWells <= 24 )
		{
			wellDimensions[ 0 ] = 6;
			wellDimensions[ 1 ] = 4;
		}
		else if ( numWells <= 96  )
		{
			wellDimensions[ 0 ] = 12;
			wellDimensions[ 1 ] = 8;
		}
		else if ( numWells <= 384  )
		{
			wellDimensions[ 0 ] = 24;
			wellDimensions[ 1 ] = 16;
		}
		else
		{
			throw new RuntimeException( "Could not determine the well dimensions." );
		}

		return wellDimensions;
	}

	public String getName()
	{
		return new File( hcsDirectory ).getName();
	}

	public boolean isTimelapse()
	{
		return isTimelapse;
	}

	public boolean isVolume()
	{
		return isVolume;
	}

	public HCSPattern getHcsPattern()
	{
		return hcsPattern;
	}
}
