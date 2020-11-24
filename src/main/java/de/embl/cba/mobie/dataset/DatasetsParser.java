package de.embl.cba.mobie.dataset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.utils.JsonUtils;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class DatasetsParser
{
	public Datasets fetchProjectDatasets( String dataSourceLocation )
	{
		try
		{
			return datasetsFromFile( dataSourceLocation + "/datasets.json" );
		}
		catch ( Exception e )
		{
			try
			{
				return datasetsFromFile( dataSourceLocation + "/versions.json" );
			}
			catch ( Exception e2 )
			{
				e.printStackTrace();
				throw new UnsupportedOperationException( "Could not open or parse datasets file: " + dataSourceLocation + "/datasets.json"  );
			}
		}
	}

	private Datasets datasetsFromFile( String path ) throws IOException
	{
		InputStream is = FileAndUrlUtils.getInputStream( path );

		try{
			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
			GsonBuilder builder = new GsonBuilder();
			Datasets datasets = builder.create().fromJson( reader, Datasets.class);
			return datasets;
		}
		catch ( Exception e )
		{
			// old version
			final Datasets datasets = new Datasets();
			datasets.datasets = JsonUtils.readStringArray( is );
			datasets.defaultDataset = datasets.datasets.get( 0 );
			return datasets;
		}
	}

	public void datasetsToFile( String path, Datasets datasets ) throws IOException
	{
		Gson gson = new GsonBuilder().create();
		OutputStream outputStream = new FileOutputStream( path );
		final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8"));
		writer.setIndent("	");
		gson.toJson(datasets, Datasets.class, writer);
		writer.close();
		outputStream.close();
	}
}
