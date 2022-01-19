package org.embl.mobie.viewer.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.io.util.FileAndUrlUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

public class DatasetJsonParser
{
	public Dataset parseDataset( String path ) throws IOException
	{
		final String datasetJson = FileAndUrlUtils.read( path );

		Gson gson = JsonHelper.buildGson( false );

		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( datasetJson, type );
		return dataset;
	}

	public void saveDataset( Dataset dataset, String path ) throws IOException {
		Gson gson = JsonHelper.buildGson( false );
		Type type = new TypeToken< Dataset >() {}.getType();

		try (OutputStream outputStream = new FileOutputStream( path );
			 final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
			writer.setIndent("  ");
			gson.toJson(dataset, type, writer);
		}
	}

	public String datasetToJsonString( Dataset dataset, boolean prettyPrinting ) {
		Gson gson = JsonHelper.buildGson( prettyPrinting );
		Type type = new TypeToken<Dataset>() {}.getType();
		return gson.toJson(dataset, type);
	}

}
