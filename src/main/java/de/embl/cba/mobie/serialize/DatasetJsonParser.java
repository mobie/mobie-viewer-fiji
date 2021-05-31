package de.embl.cba.mobie.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie.Dataset;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import static de.embl.cba.mobie.serialize.JsonHelper.buildGson;

public class DatasetJsonParser
{
	public Dataset parseDataset( String path ) throws IOException
	{
		final String datasetJson = FileAndUrlUtils.read( path );

		Gson gson = buildGson( false );

		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( datasetJson, type );
		return dataset;
	}

	public void saveDataset( Dataset dataset, String path ) throws IOException {
		Gson gson = buildGson( false );
		Type type = new TypeToken< Dataset >() {}.getType();

		try (OutputStream outputStream = new FileOutputStream( path );
			 final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
			writer.setIndent("	");
			gson.toJson(dataset, type, writer);
		}
	}

	public String datasetToJsonString( Dataset dataset, boolean prettyPrinting ) {
		Gson gson = buildGson( prettyPrinting );
		Type type = new TypeToken<Dataset>() {}.getType();
		return gson.toJson(dataset, type);
	}

}
