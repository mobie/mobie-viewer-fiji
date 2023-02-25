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
	private HashMap< String, Map< String, Map< String, String > > > siteTZPath;

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

			String channel = hcsPattern.getChannel();
			if ( ! channelWellSites.containsKey( channel ) )
			{
				final HashMap< String, Set< String > > WELL_TO_SITES = new HashMap<>();
				channelWellSites.put( channel, WELL_TO_SITES );
			}

			String well = hcsPattern.getWell();
			if ( ! channelWellSites.get( channel ).containsKey( well ) )
			{
				final HashSet< String > sites = new HashSet<>();
				channelWellSites.get( channel ).put( well, sites );
			}

			final String site = hcsPattern.getSite();
			channelWellSites.get( channel ).get( well ).add( site );

			// add the path for the site's z-plane and timepoint
			final String siteKey = getSiteKey( channel, well, site );
			if ( ! siteTZPath.containsKey( siteKey ) )
			{
				final Map< String, Map< String, String > > tzp = new LinkedHashMap();
				siteTZPath.put( siteKey, tzp );
			}

			final String t = hcsPattern.getT();
			if ( ! siteTZPath.get( siteKey ).containsKey( t ) )
			{
				final Map< String, String > zp = new LinkedHashMap();
				siteTZPath.get( siteKey ).put( t, zp );
			}

			final String z = hcsPattern.getZ();
			siteTZPath.get( siteKey ).get( t ).put( z, path );
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

	public boolean hasZorT()
	{
		return hcsPattern.hasZ() || hcsPattern.hasT();
	}
}
