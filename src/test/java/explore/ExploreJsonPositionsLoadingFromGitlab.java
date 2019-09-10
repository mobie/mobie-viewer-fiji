package explore;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.embl.cba.platynereis.platybrowser.Bookmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class ExploreJsonPositionsLoadingFromGitlab
{

	public static void main( String[] args ) throws IOException
	{
		URL url = new URL("https://git.embl.de/tischer/platy-browser-tables/raw/master/data/0.4.0/misc/bookmarks.json");

		final InputStream is = url.openStream();
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

		reader.beginObject();

		final ArrayList< Bookmark > bookmarks = new ArrayList<>();

		String name;
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
			}
		}
		catch ( IllegalStateException e )
		{
			final String x = e.toString();
			System.err.println( x );
		}

		reader.close();
	}

	public static void addPositionOrView( JsonReader reader, Bookmark bookmark ) throws IOException
	{
		String name;
		name = reader.nextName();
		if ( name.equals( "Position") )
		{
			bookmark.position = new double[ 3 ];
			reader.beginArray();
			for ( int i = 0; i < 3; i++ )
				bookmark.position[ i ] = reader.nextDouble();
			reader.endArray();
			int a =1;
		}
		else
		{
			bookmark.view = new double[ 12 ];
			reader.beginArray();
			for ( int i = 0; i < 12; i++ )
				bookmark.view[ i ] = reader.nextDouble();
			reader.endArray();
			int a =1;

		}
	}
}
