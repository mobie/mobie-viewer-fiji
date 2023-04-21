package org.embl.mobie.lib.create;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
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

	private final String jsonPath;
	private final String schemaURL;

	public JSONValidator( String jsonPath, String schemaURL )
	{
		this.jsonPath = jsonPath;
		this.schemaURL = schemaURL;
	}

	public boolean validate()
	{
		// Load the JSON file to validate
		File jsonFile = new File( jsonPath );
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode;
		try {
			jsonNode = objectMapper.readTree(jsonFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to read JSON file: " + jsonFile.getAbsolutePath());
		}

		JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
		JsonSchema schema;
		try {
			schema = schemaFactory.getJsonSchema(schemaURL);
		} catch (ProcessingException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to parse schema: " + schemaURL);
		}

		// Validate the JSON against the schema
		try {
			final ProcessingReport report = schema.validate( jsonNode );
			final boolean success = report.isSuccess();
			if (!success)
				System.out.println(report);
			return success;
		} catch (ProcessingException e) {
			System.out.println("JSON is invalid: " + e.getMessage());
			return false;
		}
	}

	public static boolean validateJSON( String jsonPath, String schemaURL )
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

			// library only supports up to draft 7 json schema - specify here, otherwise errors when reads 2020-12 in
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
				System.out.println(e.getMessage());
				e.getCausingExceptions().stream()
						.map(ValidationException::getMessage)
						.forEach(System.out::println);
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

