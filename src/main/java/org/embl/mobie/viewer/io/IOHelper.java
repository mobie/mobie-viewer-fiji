package org.embl.mobie.viewer.io;

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

	public static String[] getPaths( String imagePath ) throws IOException
	{
		final String regExPath = imagePath;

		final String dir = new File( regExPath ).getParent();
		String name = new File( regExPath ).getName();
		final String regex = wildcardToRegex( name );

		final String[] paths = Files.find( Paths.get( dir ), 999,
				( path, basicFileAttribute ) -> basicFileAttribute.isRegularFile()
						&& path.getFileName().toString().matches( regex ) ).map( path -> path.toString() ).collect( Collectors.toList() ).toArray( new String[ 0 ] );

		Arrays.sort( paths );

		return paths;
	}
}
