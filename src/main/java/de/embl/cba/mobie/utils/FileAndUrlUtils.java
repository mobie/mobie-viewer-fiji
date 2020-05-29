package de.embl.cba.mobie.utils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class FileAndUrlUtils
{
	public static String getName( String path )
	{
		if ( path.startsWith( "http" ) )
		{
			final String[] split = path.split( "/" );
			return split[ split.length - 1 ];
		}
		else
		{
			return new File( path ).getName();
		}
	}
}
