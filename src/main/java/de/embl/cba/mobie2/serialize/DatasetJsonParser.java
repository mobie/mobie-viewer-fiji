package de.embl.cba.mobie2.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.transform.AbstractSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.tables.FileAndUrlUtils;
import sc.fiji.bdvpg.services.serializers.RuntimeTypeAdapterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class DatasetJsonParser
{
	public Dataset parseDataset( String path ) throws IOException
	{
		final String datasetJson = FileAndUrlUtils.read( path );

		GsonBuilder gb = new GsonBuilder();
		Type collectionType = new TypeToken<List<SourceTransformer>>(){}.getType();
		gb.registerTypeAdapter( collectionType, new SourceTransformerDeserializer());

		Gson gson = gb.create();
		Type type = new TypeToken< Dataset >() {}.getType();
		Dataset dataset = gson.fromJson( datasetJson, type );
		return dataset;
	}
}
