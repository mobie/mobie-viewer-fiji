package org.embl.mobie.lib.hcs;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.io.ZPosition;

import java.util.LinkedHashMap;
import java.util.Map;

public class SiteStorageLocation extends StorageLocation
{
	private Map< TPosition, Map< ZPosition, String > > paths = new LinkedHashMap();

	public void addPath( TPosition t, ZPosition z, String path )
	{
		if ( ! paths.containsKey( t ) )
		{
			paths.put( t, new LinkedHashMap<>() );
		}

		paths.get( t ).put( z, path );
	}

	public void addPath( String t, String z, String path )
	{
		final TPosition tPosition = new TPosition( t );
		final ZPosition zPosition = new ZPosition( z );
		addPath( tPosition, zPosition, path );
	}
}
