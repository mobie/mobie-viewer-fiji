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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SourceDataMapAdapter implements JsonSerializer< Map< String, Data > >, JsonDeserializer< Map< String, Data > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("image", ImageSource.class);
		classToName.put(ImageSource.class.getName(), "image");
		nameToClass.put("segmentation", SegmentationSource.class);
		classToName.put(SegmentationSource.class.getName(), "segmentation");
		nameToClass.put("imageAnnotation", ImageAnnotationSource.class);
		classToName.put(ImageAnnotationSource.class.getName(), "imageAnnotation");
		nameToClass.put("spots", ImageAnnotationSource.class);
		classToName.put(ImageAnnotationSource.class.getName(), "imageAnnotation");
	}

	@Override
	public Map< String, Data > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		final HashMap< String, Data > map = new HashMap<>();
		final JsonObject jo = json.getAsJsonObject();
		for ( Map.Entry<String, JsonElement> entry : jo.entrySet() )
		{
			final Data data = ( Data ) JsonHelper.createObjectFromJsonValue( context, entry.getValue(), nameToClass );
			map.put( entry.getKey(), data );
		}
		return map;
	}

	@Override
	public JsonElement serialize( Map< String, Data > sources, Type type, JsonSerializationContext context ) {
		// TODO
		throw new UnsupportedOperationException("Serialization of Map< String, Data > is not yet implemented.");
		// return null;
	}
}
