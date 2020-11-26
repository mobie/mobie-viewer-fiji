package de.embl.cba.mobie.image;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
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

	public void writeImagePropertiesMap( String path, Map<String, ImageProperties> imagePropertiesMap ) {

		try {
			final JsonWriter writer = getJsonWriter( path );
			writeJson( writer, imagePropertiesMap );
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
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

	private void writeJson ( JsonWriter writer, Map<String, ImageProperties> imagePropertiesMap) {
		Gson gson = new Gson();
		Type type = new TypeToken< Map< String, ImageProperties > >() {}.getType();
		gson.toJson( imagePropertiesMap, type, writer);
	}

	private JsonReader getJsonReader() throws IOException
	{
		final String imagesJsonLocation = FileAndUrlUtils.combinePath( imagesLocation, "images/images.json" );

		InputStream is = FileAndUrlUtils.getInputStream( imagesJsonLocation );

		return new JsonReader( new InputStreamReader( is, "UTF-8" ) );
	}

	private JsonWriter getJsonWriter( String path ) throws IOException
	{
		OutputStream outputStream = new FileOutputStream( path );
		final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8"));
		writer.setIndent("	");

		return writer;
	}
}
