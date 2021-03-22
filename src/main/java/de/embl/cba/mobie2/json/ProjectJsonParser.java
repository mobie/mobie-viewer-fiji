package de.embl.cba.mobie2.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie2.Project;
import de.embl.cba.tables.FileAndUrlUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class ProjectJsonParser
{
	public Project parse( String path ) throws IOException
	{
		final InputStream inputStream = FileAndUrlUtils.getInputStream( path );
		final String s = IOUtils.toString( inputStream, StandardCharsets.UTF_8.name() );
		Gson gson = new Gson();
		Type type = new TypeToken< Project >() {}.getType();
		Project project = gson.fromJson( s, type );
		return project;
	}

}
