package org.embl.mobie.lib.create;

import java.io.File;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.embl.mobie.io.util.IOHelper;

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
}

