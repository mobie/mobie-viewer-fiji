/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.create;

import org.embl.mobie.io.util.IOHelper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class JSONValidator
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

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
				printValidationErrors( e );
				System.out.println("End Json Schema Errors.");

				return false;
			}
		} catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException ioException )
		{
			ioException.printStackTrace();
		}
		return true;
	}

	private static void printValidationErrors( ValidationException e ) {
		if (e.getCausingExceptions().isEmpty()) {
			// This is a leaf node: print the actual error
			System.out.println("At: " + e.getPointerToViolation());
			System.out.println("Error: " + e.getMessage());
		} else {
			// Recurse for all child exceptions
			for (ValidationException ve : e.getCausingExceptions()) {
				printValidationErrors(ve);
			}
		}
	}
}

