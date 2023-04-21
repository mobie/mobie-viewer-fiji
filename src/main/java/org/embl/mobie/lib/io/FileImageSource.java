package org.embl.mobie.lib.io;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class FileImageSource
{
	public String name;
	public String path;
	public Integer channelIndex = 0;

	public FileImageSource( String string )
	{
		// "path=name;channelIndex"

		String[] split = new String[]{ string };
		if ( string.contains( ";" ) )
		{
			split = string.split( ";" );
			channelIndex = Integer.parseInt( split[ 1 ] );
		}

		if ( split[ 0 ].contains( "=" ) )
		{
			split = split[ 0 ].split( "=" );
			path = split[ 0 ];
			name = split[ 1 ];
		}
		else
		{
			name = FilenameUtils.removeExtension( new File( split[ 0 ] ).getName() );
			path = split[ 0 ];
		}
	}
}
