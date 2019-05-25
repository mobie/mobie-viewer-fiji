package de.embl.cba.platynereis.bigdataserver;

import de.embl.cba.platynereis.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class DataSetPreparer
{

	final File inputDirectory;
	final File outputFile;

	public DataSetPreparer(
			File inputDirectory,
			File outputFile )
	{
		this.inputDirectory = inputDirectory;
		this.outputFile = outputFile;
	}

	public void run()
	{

		if ( outputFile.exists() ) outputFile.delete();

		final List< String > files = FileUtils.getFiles( inputDirectory, ".*.xml" );

		final ArrayList< String > lines = new ArrayList<>();

		for ( String file : files )
		{
			final String linuxPath = file.replace( "/Volumes/", "/g/" );
			final String fileName = new File( file ).getName();
			lines.add( fileName + "\t" + linuxPath );
		}

		System.out.println( "Writing " + lines.size() + " datasets to " + outputFile + "...");
		try
		{
			Files.write( Paths.get( outputFile.toString() ),
					lines,
					StandardCharsets.UTF_8,
					StandardOpenOption.CREATE );
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
		System.out.println( "...done!");

	}

	public static void main( String[] args )
	{
		final DataSetPreparer preparer = new DataSetPreparer(
				new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" ),
				new File( "/Volumes/cba/tischer/bigdataserver/datasets.txt" ) );
		preparer.run();
	}

}
