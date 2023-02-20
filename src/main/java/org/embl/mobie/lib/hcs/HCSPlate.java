package org.embl.mobie.lib.hcs;

import org.embl.mobie.lib.io.IOHelper;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class HCSPlate
{
	private HCSPattern hcsPattern;
	private HashMap< String, Map< String, Set< String > > > plateMap;
	private HashMap< String, String > siteToPath;

	public HCSPlate( String hcsDirectory ) throws IOException
	{
		final String[] paths = IOHelper.getPaths( hcsDirectory, 999 );

		hcsPattern = determineHCSPattern( hcsDirectory, paths );

		buildPlateMap( paths );
	}

	private void buildPlateMap( String[] paths )
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

			String well = channel + "-" + matcher.group( HCSPattern.WELL );
			if ( ! plateMap.get( channel ).containsKey( well ) )
			{
				final HashSet< String > sites = new HashSet<>();
				plateMap.get( channel ).put( well, sites );
			}

			final String site = well + "-" + matcher.group( HCSPattern.SITE );
			plateMap.get( channel ).get( well ).add( site );

			siteToPath.put( site, path );
		}
	}

	private HCSPattern determineHCSPattern( String hcsDirectory, String[] paths )
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


}
