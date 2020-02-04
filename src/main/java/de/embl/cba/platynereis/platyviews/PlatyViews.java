package de.embl.cba.platynereis.platyviews;


import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.embl.cba.platynereis.platybrowser.Bookmark;
import de.embl.cba.platynereis.utils.FileUtils;
import de.embl.cba.platynereis.utils.Version;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	private JsonToken peek;
	private String nextName;

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
				readViewsFromJsonV1( viewsSourcePath );
			}
			else
			{
				readViewsFromJsonV0( viewsSourcePath );
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

	private void readViewsFromJsonV0( String jsonFilePath ) throws IOException
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

	private void readViewsFromJsonV1( String jsonFilePath ) throws IOException
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
				addLayers( reader, bookmark );
				addPositionsAndViews( reader, bookmark );
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

	private void addPositionsAndViews( JsonReader reader, Bookmark bookmark ) throws IOException
	{
		addPositionOrView( reader, bookmark );
		if ( reader.peek().equals( JsonToken.NAME ) )
			addPositionOrView( reader, bookmark );
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

	public void addLayers( JsonReader reader, Bookmark bookmark ) throws IOException
	{
		if ( reader.nextName().equals( "Layers") )
		{
			reader.beginObject();

			while ( ! reader.peek().equals( JsonToken.END_OBJECT ) )
				addImageLayer( reader, bookmark );

			reader.endObject();
		}
		else
		{
			throw new UnsupportedOperationException("Error during Json parsing");
		}
	}

	public void addImageLayer( JsonReader reader, Bookmark bookmark ) throws IOException
	{
		final ImageLayer imageLayer = new ImageLayer();
		imageLayer.imageSourceName = reader.nextName();
		addImageLayerProperties( reader, imageLayer );
		bookmark.imageLayers.add( imageLayer );
	}

	public void addImageLayerProperties( JsonReader reader, ImageLayer imageLayer ) throws IOException
	{
		reader.beginObject();

		while ( ! reader.peek().equals( JsonToken.END_OBJECT ) )
			addImageLayerProperty( reader, imageLayer );

		reader.endObject();
	}

	// TODO: fetch what is possible with Metadata instead of ImageLayer
	public void addImageLayerProperty( JsonReader reader, ImageLayer imageLayer ) throws IOException
	{
		nextName = reader.nextName();
		if( nextName.equals( "SelectedIds" ) )
		{
			reader.beginArray();
			while ( reader.peek().equals( JsonToken.NUMBER ) )
				imageLayer.selectedIds.add( reader.nextInt() );
			reader.endArray();
		}
		else if ( nextName.equals( "Color" ) )
		{
			imageLayer.color = reader.nextString();
		}
		else if ( nextName.equals( "MaxValue") )
		{
			imageLayer.colorLutMax = reader.nextInt();
		}
		else if ( nextName.equals( "MinValue") )
		{
			imageLayer.colorLutMin = reader.nextInt();
		}
	}


}
