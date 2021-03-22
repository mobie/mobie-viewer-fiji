package de.embl.cba.mobie2.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.Project;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.IOException;
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
}
