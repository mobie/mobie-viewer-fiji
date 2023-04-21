package org.embl.mobie.lib.create;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embl.mobie.io.util.IOHelper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONValidator
{
	public static String datasetSchemaURL = "https://raw.githubusercontent.com/mobie/mobie.github.io/master/schema/dataset.schema.json";
	public static String projectSchemaURL = "https://raw.githubusercontent.com/mobie/mobie.github.io/master/schema/project.schema.json";
	public static String viewSchemaURL = "https://raw.githubusercontent.com/mobie/mobie.github.io/master/schema/view.schema.json";

	public static boolean validate( String jsonPath, String schemaURL )
	{
		JSONObject datasetSchema;
		try( InputStream schemaInputStream = IOHelper.getInputStream(
				schemaURL ) ) {
			datasetSchema = new JSONObject(new JSONTokener(schemaInputStream));
		} catch ( IOException ioException )
		{
			ioException.printStackTrace();
			throw new RuntimeException( ioException );
		}

		try ( InputStream jsonInputStream = new FileInputStream( jsonPath ) )
		{
			JSONObject jsonSubject = new JSONObject( new JSONTokener( jsonInputStream ) );

			// library only supports up to draft 7 json schema
			// specify here, otherwise errors when reads 2020-12 in
			// the schema file
			SchemaLoader loader = SchemaLoader.builder()
					.schemaJson( datasetSchema )
					.draftV7Support()
					.build();

			Schema schema = loader.load().build();
			try
			{
				schema.validate( jsonSubject );
			}
			catch ( ValidationException e )
			{
				System.out.println("Json Schema validation failed:");
				System.out.println(e.getMessage());
				e.getCausingExceptions().stream()
						.map(ValidationException::getMessage)
						.forEach(System.out::println);
				System.out.println("End Json Schema Errors:");
				return false;
			}
		} catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		} catch ( IOException ioException )
		{
			ioException.printStackTrace();
		}
		return true;
	}
}

