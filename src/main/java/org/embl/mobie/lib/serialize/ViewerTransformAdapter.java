/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.embl.mobie.lib.transform.viewer.AffineViewerTransform;
import org.embl.mobie.lib.transform.viewer.NormalVectorViewerTransform;
import org.embl.mobie.lib.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.transform.viewer.PositionViewerTransform;
import org.embl.mobie.lib.transform.viewer.TimepointViewerTransform;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

// TODO: maybe we don't need the serializer?!
public class ViewerTransformAdapter implements JsonSerializer< ViewerTransform >, JsonDeserializer< ViewerTransform >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("affine", AffineViewerTransform.class);
		classToName.put( AffineViewerTransform.class.getName(), "affine");
		nameToClass.put("normalizedAffine", NormalizedAffineViewerTransform.class);
		classToName.put( NormalizedAffineViewerTransform.class.getName(), "normalizedAffine");
		nameToClass.put("position", PositionViewerTransform.class);
		classToName.put( PositionViewerTransform.class.getName(), "position");
		nameToClass.put("timepoint", TimepointViewerTransform.class);
		classToName.put( TimepointViewerTransform.class.getName(), "timepoint");
		nameToClass.put("normalVector", NormalVectorViewerTransform.class);
		classToName.put( NormalVectorViewerTransform.class.getName(), "normalVector");
	}

	@Override
	public ViewerTransform deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		final ViewerTransform viewerTransform = ( ViewerTransform ) JsonHelper.createObjectFromJsonObject( context, json, nameToClass );
		return viewerTransform;
	}

	@Override
	public JsonElement serialize( ViewerTransform viewerTransform, Type type, JsonSerializationContext context ) {
		return context.serialize( viewerTransform );
	}
}
