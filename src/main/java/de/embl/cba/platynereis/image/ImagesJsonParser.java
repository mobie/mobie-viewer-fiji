package de.embl.cba.platynereis.image;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.embl.cba.platynereis.bookmark.Bookmark;
import de.embl.cba.platynereis.github.GithubUtils;
import de.embl.cba.platynereis.github.RepoUrlAndPath;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubContentGetter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ImagesJsonParser
{
	private final String imagesLocation;

	public ImagesJsonParser( String imagesLocation )
	{
		this.imagesLocation = imagesLocation;
	}

	public Map< String, ImageProperties > getImages()
	{
		try
		{
			return parseImages();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	private Map< String, ImageProperties > parseImages() throws IOException
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
