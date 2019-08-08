package de.embl.cba.platynereis.remote;

import com.google.gson.stream.JsonReader;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class RemoteUtils
{

	public static Map< String, String > getDatasetUrlMap( final String remoteUrl ) throws IOException
	{
		Map< String, String > datasetUrlMap = new HashMap<>();

		// Get JSON string from the server
		final URL url = new URL( remoteUrl ); //+ "/json/" );

		final InputStream is = url.openStream();
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

		reader.beginObject();

		while ( reader.hasNext() )
		{
			// skipping id
			reader.nextName();

			reader.beginObject();

			String id = null, description = null, thumbnailUrl = null, datasetUrl = null;
			while ( reader.hasNext() )
			{
				final String name = reader.nextName();
				if ( name.equals( "id" ) )
					id = reader.nextString();
				else if ( name.equals( "description" ) )
					description = reader.nextString();
				else if ( name.equals( "thumbnailUrl" ) )
					thumbnailUrl = reader.nextString();
				else if ( name.equals( "datasetUrl" ) )
					datasetUrl = reader.nextString();
				else
					reader.skipValue();
			}

			if ( id != null )
			{
				// nameList.add( id );
//				if ( thumbnailUrl != null && StringUtils.isNotEmpty( thumbnailUrl ) )
//					imageMap.put( id, new ImageIcon( new URL( thumbnailUrl ) ) );
				if ( datasetUrl != null )
					datasetUrlMap.put( id, datasetUrl );
			}

			reader.endObject();
		}

		reader.endObject();

		reader.close();

		return datasetUrlMap;
	}
}
