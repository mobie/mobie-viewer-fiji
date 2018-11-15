package de.embl.cba.platynereis.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellularModelImageCreator
{
	public static void main( String[] args ) throws IOException
	{
		final String table = "/Users/tischer/Documents/detlev-arendt-clem-registration/data/CellModels_coordinates.tsv";

		Pattern coordinate = Pattern.compile(".*( [0-9]{2,3}, [0-9]{2,3}, [0-9]{2,3} ).*");

		final HashMap< String, ArrayList< int[] > > cellModelMap = new HashMap<>();

		try( BufferedReader br = new BufferedReader( new FileReader(table) ) )
		{

			for(String line; (line = br.readLine()) != null; )
			{
				System.out.println( line );

				// Cellular model name

				String[] split = line.split( "   " );
				final String cmName = split[ 0 ];

				// Coordinates

				final ArrayList< int[] > coordinates = new ArrayList<>();
				split = line.split( " ; " );
				for ( String s : split )
				{
					Matcher m = coordinate.matcher( s );
					if ( m.matches() )
					{
						final String[] sc = m.group( 1 ).split( ", " );
						final int[] c = new int[ 3 ];
						for ( int d = 0; d < 3; ++d )
						{
							c[ d ] = Integer.parseInt( sc[ d ].trim() );
						}
						coordinates.add( c );
					}
				}

				cellModelMap.put( cmName, coordinates );

				break;
			}
		}

	}
}
