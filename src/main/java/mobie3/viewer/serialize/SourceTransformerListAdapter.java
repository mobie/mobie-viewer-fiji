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
package mobie3.viewer.serialize;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import mobie3.viewer.transform.AffineImageTransformer;
import mobie3.viewer.transform.CropImageTransformer;
import mobie3.viewer.transform.MergedGridImageTransformer;
import mobie3.viewer.transform.ImageTransformer;
import mobie3.viewer.transform.TimepointsImageTransformer;
import mobie3.viewer.transform.TransformedGridImageTransformer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SourceTransformerListAdapter implements JsonSerializer< List< ImageTransformer > >, JsonDeserializer< List< ImageTransformer > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("mergedGrid", MergedGridImageTransformer.class);
		classToName.put( MergedGridImageTransformer.class.getName(), "mergedGrid");
		nameToClass.put("transformedGrid", TransformedGridImageTransformer.class);
		classToName.put( TransformedGridImageTransformer.class.getName(), "transformedGrid");
		nameToClass.put("affine", AffineImageTransformer.class);
		classToName.put( AffineImageTransformer.class.getName(), "affine");
		nameToClass.put("timepoints", TimepointsImageTransformer.class);
		classToName.put( TimepointsImageTransformer.class.getName(), "timepoints");
		nameToClass.put("crop", CropImageTransformer.class);
		classToName.put( CropImageTransformer.class.getName(), "crop");
	}

	@Override
	public List< ImageTransformer > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		List list = new ArrayList< ImageTransformer >();
		JsonArray ja = json.getAsJsonArray();

		for ( JsonElement je : ja )
		{
			list.add( JsonHelper.createObjectFromJsonValue( context, je, nameToClass ) );
		}

		return list;
	}

	@Override
	public JsonElement serialize( List< ImageTransformer > imageTransformers, Type type, JsonSerializationContext context ) {
		JsonArray ja = new JsonArray();
		for ( ImageTransformer imageTransformer : imageTransformers ) {
			Map< String, ImageTransformer > nameToTransformer = new HashMap<>();
			nameToTransformer.put( classToName.get( imageTransformer.getClass().getName() ), imageTransformer );

			if ( imageTransformer instanceof TransformedGridImageTransformer ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, TransformedGridImageTransformer > >() {}.getType() ) );
			} else if ( imageTransformer instanceof AffineImageTransformer ) {
				ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, AffineImageTransformer > >() {}.getType() ) );
			} else if ( imageTransformer instanceof CropImageTransformer ) {
				ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, CropImageTransformer > >() {}.getType() ) );
			} else if ( imageTransformer instanceof MergedGridImageTransformer ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, MergedGridImageTransformer > >() {}.getType() ) );
			} else if ( imageTransformer instanceof TimepointsImageTransformer ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, TimepointsImageTransformer > >(){}.getType()) );
			} else {
				throw new UnsupportedOperationException( "Could not serialise SourceTransformer of type: " + imageTransformer.getClass().toString() );
			}
		}

		return ja;
	}
}
