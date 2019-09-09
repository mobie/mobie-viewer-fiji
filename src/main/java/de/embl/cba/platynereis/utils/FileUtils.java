package de.embl.cba.platynereis.utils;

import de.embl.cba.platynereis.remote.RemoteUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileUtils
{
	public static List< File > getFileList( File directory, String fileNameRegExp )
	{
		final ArrayList< File > files = new ArrayList<>();
		populateFileList( directory, fileNameRegExp,files );
		return files;
	}

	public static void populateFileList( File directory, String fileNameRegExp, List< File > files ) {

		// Get all the files from a directory.
		File[] fList = directory.listFiles();

		if( fList != null )
		{
			for ( File file : fList )
			{
				if ( file.isFile() )
				{
					final Matcher matcher = Pattern.compile( fileNameRegExp ).matcher( file.getName() );

					if ( matcher.matches() )
						files.add( file );
				}
				else if ( file.isDirectory() )
				{
					populateFileList( file, fileNameRegExp, files );
				}
			}
		}
	}

	public static List< String > getFiles( File inputDirectory, String filePattern )
	{
		final List< File > fileList =
				de.embl.cba.tables.FileUtils.getFileList(
						inputDirectory, filePattern, false );

		Collections.sort( fileList, new FileUtils.SortFilesIgnoreCase() );

		final List< String > paths = fileList.stream().map( x -> x.toString() ).collect( Collectors.toList() );

		return paths;
	}

	public static List< String > getUrls( String dataFolder )
	{
		try
		{
			final Map< String, String > datasetUrlMap
					= RemoteUtils.getDatasetUrlMap( dataFolder );

			return new ArrayList( datasetUrlMap.values() );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	public static String getSeparator( String location )
	{
		String separator = null;
		if ( location.startsWith( "http" ) )
			separator = "/";
		else
			separator = File.separator;
		return separator;
	}

	public static String combinePath( String... paths )
	{
		final String separator = getSeparator( paths[ 0 ] );

		String combined = paths[ 0 ];
		for ( int i = 1; i < paths.length; i++ )
			combined = combined + separator + paths[ i ];

		return combined;
	}

	public static String removeTrailingSlash( String path )
	{
		if ( path.endsWith( "/" ) ) path = path.substring(0, path.length() - 1);
		return path;
	}

	public static class SortFilesIgnoreCase implements Comparator<File>
	{
		public int compare( File o1, File o2 )
		{
			String s1 = o1.getName();
			String s2 = o2.getName();
			return s1.toLowerCase().compareTo(s2.toLowerCase());
		}
	}

}
