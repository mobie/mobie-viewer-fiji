package de.embl.cba.mobie.serialize;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.mobie.Project;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

public class ProjectJsonParser
{
	public Project parseProject( String path ) throws IOException
	{
		final String s = FileAndUrlUtils.read( path );
		Gson gson = new Gson();
		Type type = new TypeToken< Project >() {}.getType();
		Project project = gson.fromJson( s, type );
		return project;
	}

	public void saveProject( Project project, String path ) throws IOException {
		Gson gson = new Gson();
		Type type = new TypeToken< Project >() {}.getType();

		try (OutputStream outputStream = new FileOutputStream( path );
			 final JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
			writer.setIndent("	");
			gson.toJson( project, type, writer );
		}
	}
}
