package org.embl.mobie.lib.io;

public class TableImageSource
{
	public String name;
	public String columnName;
	public Integer channelIndex = 0;

	public TableImageSource( String string )
	{
		// "columnName=image;channelIndex"

		String[] split = new String[]{ string };
		if ( string.contains( ";" ) )
		{
			split = string.split( ";" );
			channelIndex = Integer.parseInt( split[ 1 ] );
		}

		if ( split[ 0 ].contains( "=" ) )
		{
			split = split[ 0 ].split( "=" );
			columnName = split[ 0 ];
			name = split[ 1 ];
		}
		else
		{
			name = split[ 0 ];
			columnName = split[ 0 ];
		}
	}
}
