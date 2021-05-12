package de.embl.cba.mobie2.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

public class DatasetJsonParser
{
	public Dataset getDataset( String path ) throws IOException
	{
		final String s = FileAndUrlUtils.read( path );
		Gson gson = new Gson();
		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( s, type );
		return dataset;
	}

	public void saveDataset( Dataset dataset, String path ) throws IOException {
		Gson gson = new Gson();
		Type type = new TypeToken< Dataset >() {}.getType();

		try (OutputStream outputStream = new FileOutputStream( path );
			 final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
			writer.setIndent("	");
			gson.toJson(dataset, type, writer);
		}
	}

	public String datasetToJsonString( Dataset dataset, boolean prettyPrinting ) {
		Gson gson;
		if ( prettyPrinting ) {
			gson = new GsonBuilder().setPrettyPrinting().create();
		} else {
			gson = new Gson();
		}
		Type type = new TypeToken< Dataset >() {}.getType();
		return gson.toJson( dataset, type );
	}
}
