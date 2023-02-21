package org.embl.mobie.lib.io;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class IOHelper
{
	// from https://stackoverflow.com/questions/28734455/java-converting-file-pattern-to-regular-expression-pattern
	public static String wildcardToRegex(String wildcard){
		StringBuffer s = new StringBuffer(wildcard.length());
		s.append('^');
		for (int i = 0, is = wildcard.length(); i < is; i++) {
			char c = wildcard.charAt(i);
			switch(c) {
				case '*':
					s.append(".*");
					break;
				case '?':
					s.append(".");
					break;
				case '^': // escape character in cmd.exe
					s.append("\\");
					break;
				// escape special regexp-characters
				case '(': case ')': case '[': case ']': case '$':
				case '.': case '{': case '}': case '|':
				case '\\':
					s.append("\\");
					s.append(c);
					break;
				default:
					s.append(c);
					break;
			}
		}
		s.append('$');
		return(s.toString());
	}

	public static String[] getPaths( String pathWithWildCards, int maxDepth ) throws IOException
	{
		final String dir = new File( pathWithWildCards ).getParent();
		String name = new File( pathWithWildCards ).getName();
		final String regex = wildcardToRegex( name );

		final String[] paths = Files.find( Paths.get( dir ), maxDepth,
				( path, basicFileAttribute ) -> basicFileAttribute.isRegularFile()
						&& path.getFileName().toString().matches( regex ) ).map( path -> path.toString() ).collect( Collectors.toList() ).toArray( new String[ 0 ] );

		Arrays.sort( paths );

		if ( paths.length == 0 )
			System.err.println("Could not find any files for " + regex );

		return paths;
	}

	public static AbstractSpimData< ? > tryOpenSpimData( String segmentationPath, ImageDataFormat imageDataFormat )
	{
		try
		{
			return new SpimDataOpener().open( segmentationPath, imageDataFormat );
		}
		catch ( SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}
}
