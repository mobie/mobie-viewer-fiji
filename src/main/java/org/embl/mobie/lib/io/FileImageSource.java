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
		String[] split = new String[]{ string };
		if ( string.contains( ";" ) )
		{
			split = string.split( ";" );
			channelIndex = Integer.parseInt( split[ 1 ] );
		}

		if ( split[ 0 ].contains( "=" ) )
		{
			split = split[ 0 ].split( "=" );
			name = split[ 0 ];
			path = split[ 1 ];
		}
		else
		{
			name = FilenameUtils.removeExtension( new File( split[ 0 ] ).getName() );
			path = split[ 0 ];
		}
	}
}
