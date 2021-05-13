package de.embl.cba.mobie2.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.IOException;
import java.lang.reflect.Type;

public class DatasetJsonParser
{
	public Dataset parseDataset( String path ) throws IOException
	{
		final String datasetJson = FileAndUrlUtils.read( path );

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter( SourceTransformer.class, new SourceTransformerDeserializer());
		Gson gson = new Gson();
		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( datasetJson, type );
		return dataset;
	}
}
