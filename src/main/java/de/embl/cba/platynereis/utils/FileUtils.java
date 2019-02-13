package de.embl.cba.platynereis.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils
{
	public static List< File > getFileList( File directory, String fileNameRegExp )
	{
		final ArrayList< File > files = new ArrayList<>();
		populateFileList( directory, fileNameRegExp,files );
		return files;
	}

	public static void populateFileList( File directory, String fileNameRegExp, List< File > files) {

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
					{
						files.add( file );
					}
				}
				else if ( file.isDirectory() )
				{
					populateFileList( file, fileNameRegExp, files );
				}
			}
		}
	}

	public static List< File > getFiles( File inputDirectory, String filePattern )
	{
		final List< File > fileList = de.embl.cba.tables.FileUtils.getFileList( inputDirectory, filePattern, false );
		Collections.sort( fileList, new FileUtils.SortFilesIgnoreCase() );
		return fileList;
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
