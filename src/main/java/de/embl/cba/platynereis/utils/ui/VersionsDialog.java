package de.embl.cba.platynereis.utils.ui;

import com.google.gson.stream.JsonReader;
import ij.gui.GenericDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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
			versions = readVersionsFromUrl( versionsJsonFilePath );
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

	private ArrayList< String > readVersionsFromUrl( String versionsJsonFilePath ) throws IOException
	{
		URL url = new URL( versionsJsonFilePath );

		final InputStream is = url.openStream();
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
