/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.serialize;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.viewer.serialize.transformation.AffineTransformation;
import org.embl.mobie.viewer.serialize.transformation.CropTransformation;
import org.embl.mobie.viewer.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.viewer.serialize.transformation.TimepointsTransformation;
import org.embl.mobie.viewer.serialize.transformation.Transformation;
import org.embl.mobie.viewer.serialize.transformation.TransformedGridTransformation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SourceTransformerListAdapter implements JsonSerializer< List< Transformation > >, JsonDeserializer< List< Transformation > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("mergedGrid", MergedGridTransformation.class);
		classToName.put( MergedGridTransformation.class.getName(), "mergedGrid");
		nameToClass.put("transformedGrid", TransformedGridTransformation.class);
		classToName.put( TransformedGridTransformation.class.getName(), "transformedGrid");
		nameToClass.put("affine", AffineTransformation.class);
		classToName.put( AffineTransformation.class.getName(), "affine");
		nameToClass.put("timepoints", TimepointsTransformation.class);
		classToName.put( TimepointsTransformation.class.getName(), "timepoints");
		nameToClass.put("crop", CropTransformation.class);
		classToName.put( CropTransformation.class.getName(), "crop");
	}

	@Override
	public List< Transformation > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		List list = new ArrayList< Transformation >();
		JsonArray ja = json.getAsJsonArray();

		for ( JsonElement je : ja )
		{
			list.add( JsonHelper.createObjectFromJsonValue( context, je, nameToClass ) );
		}

		return list;
	}

	@Override
	public JsonElement serialize( List< Transformation > imageTransformations, Type type, JsonSerializationContext context ) {
		JsonArray ja = new JsonArray();
		for ( Transformation imageTransformation : imageTransformations ) {
			Map< String, Transformation > nameToTransformer = new HashMap<>();
			nameToTransformer.put( classToName.get( imageTransformation.getClass().getName() ), imageTransformation );

			if ( imageTransformation instanceof TransformedGridTransformation ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, TransformedGridTransformation > >() {}.getType() ) );
			} else if ( imageTransformation instanceof AffineTransformation ) {
				ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, AffineTransformation > >() {}.getType() ) );
			} else if ( imageTransformation instanceof CropTransformation ) {
				ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, CropTransformation > >() {}.getType() ) );
			} else if ( imageTransformation instanceof MergedGridTransformation ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, MergedGridTransformation > >() {}.getType() ) );
			} else if ( imageTransformation instanceof TimepointsTransformation ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, TimepointsTransformation > >(){}.getType()) );
			} else {
				throw new UnsupportedOperationException( "Could not serialise SourceTransformer of type: " + imageTransformation.getClass().toString() );
			}
		}

		return ja;
	}
}
