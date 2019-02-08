package de.embl.cba.platynereis.utils;

import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.IntType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellularModelImageCreator
{
	public static void main( String[] args ) throws IOException
	{
		final String table = "/Users/tischer/Documents/detlev-arendt-clem-registration/data/CellModels_coordinates.tsv";

		Pattern coordinatePattern = Pattern.compile(".*( [0-9]{2,3}, [0-9]{2,3}, [0-9]{2,3} ).*");

		final HashMap< String, List< int[] > > cmMap = new HashMap<>();

		try( BufferedReader br = new BufferedReader( new FileReader(table) ) )
		{

			for(String line; (line = br.readLine()) != null; )
			{
				// System.out.println( line );

				// Cellular model name

				String[] split = line.split( "   " );
				final String cmName = split[ 0 ];

				// Coordinates

				final List< int[] > coordinates = new ArrayList<>();
				split = line.split( " ; " );
				for ( String s : split )
				{
					Matcher m = coordinatePattern.matcher( s );
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

				cmMap.put( cmName, coordinates );
			}
		}

		// put into image

		final RandomAccessibleInterval< IntType > image = ArrayImgs.ints( new long[]{ 320, 550, 1, 251 } );
		final RandomAccess< IntType > access = image.randomAccess();

		int i = 0;
		for ( String cm : cmMap.keySet() )
		{
			final int cmIndex = i++; // TODO: which index to use? Integer.parseInt( cm.split( "_" )[ 1 ] );

			final List< int[] > coordinates = cmMap.get( cm );

			for ( int[] coordinate : coordinates )
			{
				int[] correctedDimensionOrder = new int[ 4 ];
				correctedDimensionOrder[ 0 ] = coordinate[ 1 ];
				correctedDimensionOrder[ 1 ] = coordinate[ 0 ];
				correctedDimensionOrder[ 2 ] = 0; // channel
				correctedDimensionOrder[ 3 ] = coordinate[ 2 ];

				access.setPosition( correctedDimensionOrder );
				access.get().set( cmIndex );
			}
		}

		new ImageJ();
		// show image
		final ImagePlus imagePlus = ImageJFunctions.wrap( image, "cm" );
		imagePlus.show();
	}
}
