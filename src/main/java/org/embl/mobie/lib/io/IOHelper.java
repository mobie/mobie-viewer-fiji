package org.embl.mobie.lib.io;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.github.GitHubUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.embl.mobie.io.util.IOHelper.combinePath;

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
//				case '?':
//					s.append(".");
//					break;
//				case '^': // escape character in cmd.exe
//					s.append("\\");
//					break;
				// escape special regexp-characters
//				case '(': case ')': case '[': case ']': case '$':
//				case '.': case '{': case '}': case '|':
//				case '\\':
//					s.append("\\");
//					s.append(c);
//					break;
				default:
					s.append(c);
					break;
			}
		}
		s.append('$');
		return(s.toString());
	}

	public static List< String > getPaths( String regex, int maxDepth )
	{
		final String dir = new File( regex ).getParent();
		String name = new File( regex ).getName();

		try
		{
			final List< String > paths = Files.find( Paths.get( dir ), maxDepth,
					( path, basicFileAttribute ) -> basicFileAttribute.isRegularFile()
							&& path.getFileName().toString()
							.matches( name ) )
							.map( path -> path.toString() ).collect( Collectors.toList() );
			Collections.sort( paths );

			if ( paths.size() == 0 )
				System.err.println("Could not find any files for " + regex );

			return paths;

		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	public static AbstractSpimData< ? > tryOpenSpimData( String path, ImageDataFormat imageDataFormat )
	{
		try
		{
			return new SpimDataOpener().open( path, imageDataFormat );
		}
		catch ( SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static String createPath( String rootLocation, String githubBranch, String... files )
	{
		if ( rootLocation.contains( "github.com" ) )
		{
			rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
		}

		final ArrayList< String > strings = new ArrayList<>();
		strings.add( rootLocation );
		Collections.addAll( strings, files );
		final String path = combinePath( strings.toArray( new String[0] ) );

		return path;
	}

}
