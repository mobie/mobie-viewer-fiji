package de.embl.cba.platynereis.platybrowser;


import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.embl.cba.platynereis.utils.FileUtils;
import de.embl.cba.platynereis.utils.Version;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * TODO: probably read this from a editable text file so that users can add own views.
 *
 *
 */
public class PlatyViews
{
	public static final String LEFT_EYE_POSITION = "Left eye";

	private final HashMap< String, double[] > nameToView;
	private ArrayList< Bookmark > bookmarks;

	public PlatyViews()
	{
		nameToView = new HashMap<>();
		nameToView.put( LEFT_EYE_POSITION, new double[]{ 177, 218, 67, 0} );
	}

	public PlatyViews( String viewsSourcePath )
	{
		this( viewsSourcePath, "0.0.0" );
	}

	public PlatyViews( String viewsSourcePath, String versionString )
	{
		final Version version = new Version( versionString );

		nameToView = new HashMap<>();
		try
		{
			if ( version.compareTo( new Version( "0.6.5" ) ) > 0 )
			{
				readViewsFromFileV2( viewsSourcePath );
			}
			else
			{
				readViewsFromFile( viewsSourcePath );
			}
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public HashMap< String, double[] > views()
	{
		return nameToView;
	}

	private void readViewsFromFile( String jsonFilePath ) throws IOException
	{
		InputStream is = FileUtils.getInputStream( jsonFilePath );
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
		reader.beginObject();

		bookmarks = new ArrayList<>();

		try
		{
			while ( reader.hasNext() )
			{
				final Bookmark bookmark = new Bookmark();
				bookmark.name = reader.nextName();
				reader.beginObject();
				addPositionOrView( reader, bookmark );
				final JsonToken peek = reader.peek();
				if ( peek.equals( JsonToken.NAME ) )
					addPositionOrView( reader, bookmark );
				reader.endObject();

				bookmarks.add( bookmark );

				if ( bookmark.view != null )
					nameToView.put(	bookmark.name, bookmark.view );
				else
					nameToView.put(	bookmark.name, bookmark.position );
			}
		}
		catch ( Exception e )
		{
			final String x = e.toString();
			System.err.println( x );
		}

		reader.close();
	}

	private void readViewsFromFileV2( String jsonFilePath ) throws IOException
	{
		InputStream is = FileUtils.getInputStream( jsonFilePath );
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
		reader.beginObject();

		bookmarks = new ArrayList<>();

		try
		{
			while ( reader.hasNext() )
			{
				final Bookmark bookmark = new Bookmark();
				bookmark.name = reader.nextName();
				reader.beginObject();
				addPositionOrView( reader, bookmark );
				final JsonToken peek = reader.peek();
				if ( peek.equals( JsonToken.NAME ) )
					addPositionOrView( reader, bookmark );
				reader.endObject();

				bookmarks.add( bookmark );

				if ( bookmark.view != null )
					nameToView.put(	bookmark.name, bookmark.view );
				else
					nameToView.put(	bookmark.name, bookmark.position );
			}
		}
		catch ( Exception e )
		{
			final String x = e.toString();
			System.err.println( x );
		}

		reader.close();
	}

	public static void addPositionOrView( JsonReader reader, Bookmark bookmark ) throws IOException
	{
		String name = reader.nextName();
		if ( name.equals( "Position") )
		{
			bookmark.position = new double[ 3 ];
			reader.beginArray();
			for ( int i = 0; i < 3; i++ )
				bookmark.position[ i ] = reader.nextDouble();
			reader.endArray();
		}
		else
		{
			bookmark.view = new double[ 12 ];
			reader.beginArray();
			for ( int i = 0; i < 12; i++ )
				bookmark.view[ i ] = reader.nextDouble();
			reader.endArray();
		}
	}


}
