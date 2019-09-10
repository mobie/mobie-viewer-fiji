package de.embl.cba.platynereis.utils.ui;

import com.google.gson.stream.JsonReader;
import de.embl.cba.platynereis.utils.FileUtils;
import ij.gui.GenericDialog;

import java.io.*;
import java.util.ArrayList;

public class VersionsDialog
{
	public VersionsDialog()
	{
	}

	public String showDialog( String versionsJsonFilePath )
	{
		ArrayList< String > versions = null;
		try
		{
			versions = readVersionsFromFile( versionsJsonFilePath );
		} catch ( IOException e )
		{
			e.printStackTrace();
		}

		final String[] versionsArray = asArray( versions );

		final GenericDialog gd = new GenericDialog( "Data Version" );
		gd.addChoice( "Data Version", versionsArray, versionsArray[ versionsArray.length - 1 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		else return gd.getNextChoice();
	}

	private String[] asArray( ArrayList< String > versions )
	{
		final String[] versionsArray = new String[ versions.size() ];
		for ( int i = 0; i < versionsArray.length; i++ )
			versionsArray[ i ] = versions.get( i );
		return versionsArray;
	}

	private ArrayList< String > readVersionsFromFile( String versionsJsonFilePath ) throws IOException
	{
		InputStream is = FileUtils.getInputStream( versionsJsonFilePath );

		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

		reader.beginArray();

		final ArrayList< String > versions = new ArrayList< String >();
		while ( reader.hasNext() )
			versions.add( reader.nextString() );
		reader.endArray();
		reader.close();

		return versions;
	}

}
