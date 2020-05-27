package de.embl.cba.mobie.json;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class JsonUtils
{
	public static ArrayList< String > readStringArray( InputStream is ) throws IOException
	{
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

		reader.beginArray();
		final ArrayList< String > strings = new ArrayList< String >();
		while ( reader.hasNext() )
			strings.add( reader.nextString() );
		reader.endArray();
		reader.close();

		return strings;
	}
}
