package de.embl.cba.mobie2.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.List;

public class DatasetJsonParser
{
	public Dataset parseDataset( String path ) throws IOException
	{
		final String datasetJson = FileAndUrlUtils.read( path );

		Gson gson = buildGson();

		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( datasetJson, type );
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
		if (prettyPrinting) {
			gson = new GsonBuilder().setPrettyPrinting().create();
		} else {
			gson = new Gson();
		}
		Type type = new TypeToken<Dataset>() {
		}.getType();
		return gson.toJson(dataset, type);
	}

	private Gson buildGson()
	{
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter( new TypeToken<List< SourceTransformer >>(){}.getType(), new SourceTransformerListDeserializer());
		gb.registerTypeAdapter( new TypeToken<List< SourceDisplay >>(){}.getType(), new SourceDisplayListDeserializer());
		Gson gson = gb.create();
		return gson;
	}
}
