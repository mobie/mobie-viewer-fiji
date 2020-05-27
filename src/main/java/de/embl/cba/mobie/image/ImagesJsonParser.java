package de.embl.cba.mobie.image;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class ImagesJsonParser
{
	private final String imagesLocation;

	public ImagesJsonParser( String imagesLocation )
	{
		this.imagesLocation = imagesLocation;
	}

	public Map< String, ImageProperties > getImagePropertiesMap()
	{
		try
		{
			return parseImagesJson();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	private Map< String, ImageProperties > parseImagesJson() throws IOException
	{
		final JsonReader reader = getJsonReader();

		Map< String, ImageProperties > nameToImageProperties = parseJson( reader );

		return nameToImageProperties;
	}

	private Map< String, ImageProperties > parseJson( JsonReader reader )
	{
		Gson gson = new Gson();
		Type type = new TypeToken< Map< String, ImageProperties > >() {}.getType();
		return gson.fromJson( reader, type );
	}

	private JsonReader getJsonReader() throws IOException
	{
		final String imagesJsonLocation = FileAndUrlUtils.combinePath( imagesLocation, "images/images.json" );

		InputStream is = FileAndUrlUtils.getInputStream( imagesJsonLocation );

		return new JsonReader( new InputStreamReader( is, "UTF-8" ) );
	}
}
