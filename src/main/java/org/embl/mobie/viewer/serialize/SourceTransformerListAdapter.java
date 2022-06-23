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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;
import org.embl.mobie.viewer.transform.CropSourceTransformer;
import org.embl.mobie.viewer.transform.TimePointSourceTransformer;
import org.embl.mobie.viewer.transform.TransformedGridSourceTransformer;
import org.embl.mobie.viewer.transform.MergedGridSourceTransformer;
import org.embl.mobie.viewer.transform.SourceTransformer;

import java.lang.reflect.Type;
import java.util.*;

public class SourceTransformerListAdapter implements JsonSerializer< List<SourceTransformer> >, JsonDeserializer< List< SourceTransformer > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("mergedGrid", MergedGridSourceTransformer.class);
		classToName.put( MergedGridSourceTransformer.class.getName(), "mergedGrid");
		nameToClass.put("transformedGrid", TransformedGridSourceTransformer.class);
		classToName.put( TransformedGridSourceTransformer.class.getName(), "transformedGrid");
		nameToClass.put("affine", AffineSourceTransformer.class);
		classToName.put(AffineSourceTransformer.class.getName(), "affine");
		nameToClass.put("timepoints", TimePointSourceTransformer.class);
		classToName.put( TimePointSourceTransformer.class.getName(), "timepoints");
		nameToClass.put("crop", CropSourceTransformer.class);
		classToName.put(CropSourceTransformer.class.getName(), "crop");
	}

	@Override
	public List< SourceTransformer > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		List list = new ArrayList< SourceTransformer >();
		JsonArray ja = json.getAsJsonArray();

		for ( JsonElement je : ja )
		{
			list.add( JsonHelper.createObjectFromJsonValue( context, je, nameToClass ) );
		}

		return list;
	}

	@Override
	public JsonElement serialize( List<SourceTransformer> sourceTransformers, Type type, JsonSerializationContext context ) {
		JsonArray ja = new JsonArray();
		for ( SourceTransformer sourceTransformer: sourceTransformers ) {
			Map< String, SourceTransformer > nameToTransformer = new HashMap<>();
			nameToTransformer.put( classToName.get( sourceTransformer.getClass().getName() ), sourceTransformer );

			if ( sourceTransformer instanceof TransformedGridSourceTransformer ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, TransformedGridSourceTransformer > >() {}.getType() ) );
			} else if ( sourceTransformer instanceof AffineSourceTransformer ) {
				ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, AffineSourceTransformer > >() {}.getType() ) );
			} else if ( sourceTransformer instanceof CropSourceTransformer ) {
				ja.add( context.serialize( nameToTransformer , new TypeToken< Map< String, CropSourceTransformer > >() {}.getType() ) );
			} else if ( sourceTransformer instanceof MergedGridSourceTransformer ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, MergedGridSourceTransformer > >() {}.getType() ) );
			} else if ( sourceTransformer instanceof TimePointSourceTransformer ) {
				ja.add( context.serialize( nameToTransformer, new TypeToken< Map< String, TimePointSourceTransformer > >(){}.getType()) );
			} else {
				throw new UnsupportedOperationException( "Could not serialise SourceTransformer of type: " + sourceTransformer.getClass().toString() );
			}
		}

		return ja;
	}
}
