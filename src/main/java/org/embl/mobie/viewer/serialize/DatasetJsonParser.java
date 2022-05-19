package org.embl.mobie.viewer.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import ij.IJ;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.io.util.IOHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

public class DatasetJsonParser
{
	public Dataset parseDataset( String path ) throws IOException
	{
		final String datasetJson = IOHelper.read( path );
		Gson gson = JsonHelper.buildGson( false );
		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( datasetJson, type );
		return dataset;
	}

	public void saveDataset( Dataset dataset, String path ) throws IOException {
		Gson gson = JsonHelper.buildGson( true );
		final String json = gson.toJson( dataset ).replaceAll("\t", "  ");;
		IOHelper.write( path, json );
	}

	public String datasetToJsonString( Dataset dataset, boolean prettyPrinting ) {
		Gson gson = JsonHelper.buildGson( prettyPrinting );
		Type type = new TypeToken<Dataset>() {}.getType();
		return gson.toJson(dataset, type);
	}

}
