package org.embl.mobie.lib.hcs;

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
	private HCSPattern hcsPattern;
	private HashMap< String, Map< String, Set< String > > > plateMap;
	private HashMap< String, String > siteToPath;

	public HCSPlate( String hcsDirectory ) throws IOException
	{
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

//	private int[] getWellGridPosition( String well )
//	{
//		if ( namingScheme.equals( NamingSchemes.PATTERN_OPERETTA ) )
//		{
//			final Matcher matcher = Pattern.compile( "r(?<row>[0-9]{2})c(?<col>[0-9]{2})" ).matcher( well );
//			if ( ! matcher.matches() )
//				throw new RuntimeException( "Could not decode well " + well );
//
//			final int row = Integer.parseInt( matcher.group( "row" ) ) - 1;
//			final int col = Integer.parseInt( matcher.group( "col" ) ) - 1;
//			return new int[]{ row, col };
//		}
//		else
//		{
//			return Utils.getWellPositionFromA01( well );
//		}
//	}

	public int[] getSiteGridPosition( String channel, String well, String site )
	{

		switch ( hcsPattern )
		{
			default:
			case Operetta:
				int numSites = plateMap.get( channel ).get( well ).size();
				int siteIndex = Integer.parseInt( site ) - 1;
				int numSiteColumns = (int) Math.sqrt( numSites );

				int[] sitePosition = new int[ 2 ];
				sitePosition[ 0 ] = siteIndex % numSiteColumns; // column
				sitePosition[ 1 ] = siteIndex / numSiteColumns; // row

				System.out.println( "Site index = " + siteIndex + ", c = " + sitePosition[ 0 ] + ", r = " + sitePosition[ 1 ]);

				return sitePosition;
		}
	}
}
