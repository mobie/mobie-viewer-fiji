package explore;

import com.google.gson.stream.JsonReader;
import de.embl.cba.tables.Tables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class ExploreJsonLoadingFromGitlab
{
	public static void main( String[] args ) throws IOException
	{

		URL url = new URL("https://git.embl.de/tischer/platy-browser-tables/raw/master/data/versions.json");

		final InputStream is = url.openStream();
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

		reader.beginArray();

		final ArrayList< String > versions = new ArrayList< String >();
		while ( reader.hasNext() )
			versions.add( reader.nextString() );
		reader.endArray();
		reader.close();

		final String[] versionsArray = new String[ versions.size() ];
		for ( int i = 0; i < versionsArray.length; i++ )
			versionsArray[ i ] = versions.get( i );


	}
}
