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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class HCSPlate
{
	private final String hcsDirectory;
	private HCSPattern hcsPattern;
	private HashMap< String, Map< String, Set< String > > > plateMap;
	private HashMap< String, String > siteToPath;

	public HCSPlate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		final List< String > paths = Files.walk( Paths.get( hcsDirectory ) ).map( p -> p.toString() ).collect( Collectors.toList() );

		hcsPattern = determineHCSPattern( hcsDirectory, paths );

		buildPlateMap( paths );
	}

	private void buildPlateMap( List< String > paths )
	{
		plateMap = new HashMap<>();
		siteToPath = new HashMap<>();

		for ( String path : paths )
		{
			final Matcher matcher = hcsPattern.getMatcher( path );
			if ( ! matcher.matches() ) continue;

			final String channel = matcher.group( HCSPattern.CHANNEL );
			if ( ! plateMap.containsKey( channel ) )
			{
				final HashMap< String, Set< String > > WELL_TO_SITES = new HashMap<>();
				plateMap.put( channel, WELL_TO_SITES );
			}

			String well = matcher.group( HCSPattern.WELL );
			if ( ! plateMap.get( channel ).containsKey( well ) )
			{
				final HashSet< String > sites = new HashSet<>();
				plateMap.get( channel ).put( well, sites );
			}

			final String site = matcher.group( HCSPattern.SITE );
			plateMap.get( channel ).get( well ).add( site );

			siteToPath.put( getSiteKey( channel, well, site ), path );
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
		return plateMap.keySet();
	}

	public Set< String > getWells( String channel )
	{
		return plateMap.get( channel ).keySet();
	}

	public Set< String > getSites( String channel, String well )
	{
		return plateMap.get( channel ).get( well );
	}

	public String getPath( String channel, String well, String site )
	{
		return siteToPath.get( getSiteKey( channel, well, site ) );
	}

	public int[] getSiteGridPosition( String channel, String well, String site )
	{
		switch ( hcsPattern )
		{
			default:
			case Operetta:
				int numSites = plateMap.get( channel ).get( well ).size();
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

	private String getFirstSitePath( String channel )
	{
		final String firstWell = plateMap.get( channel ).keySet().iterator().next();
		final String firstSite = plateMap.get( channel ).get( firstWell ).iterator().next();
		return getPath( channel, firstWell, firstSite );
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
}
