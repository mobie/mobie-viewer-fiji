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
package org.embl.mobie.lib.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.DisplayListAdapter;
import org.embl.mobie.lib.serialize.transformation.TransformationListAdapter;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;

import java.util.List;
import java.util.Map;

public class JsonHelper
{

	public static Object createObjectFromJsonValue( JsonDeserializationContext context, JsonElement je, Map< String, Class > nameToClass )
	{
		final JsonObject jsonObject = je.getAsJsonObject();
		final Map.Entry< String, JsonElement > jsonElementEntry = jsonObject.entrySet().iterator().next();
		Class c = nameToClass.get( jsonElementEntry.getKey() );
		if (c == null)
			throw new RuntimeException("Unknown class: " + jsonElementEntry.getKey());

		try
		{
			final Object deserialize = context.deserialize( jsonElementEntry.getValue(), c );
			return deserialize;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static Object createObjectFromJsonObject( JsonDeserializationContext context, JsonElement je, Map< String, Class > nameToClass )
	{
		final JsonObject jsonObject = je.getAsJsonObject();
		final Map.Entry< String, JsonElement > jsonElementEntry = jsonObject.entrySet().iterator().next();
		Class c = nameToClass.get( jsonElementEntry.getKey() );
		if (c == null)
			throw new RuntimeException("Unknown class: " + jsonElementEntry.getKey());
		final Object deserialize = context.deserialize( jsonObject, c );
		return deserialize;
	}

	public static Gson buildGson( boolean prettyPrinting )
	{
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter( new TypeToken< List< Transformation > >(){}.getType(), new TransformationListAdapter());
		gb.registerTypeAdapter( new TypeToken< List< Display< ? > > >(){}.getType(), new DisplayListAdapter());
		gb.registerTypeAdapter( new TypeToken< ViewerTransform >(){}.getType(), new ViewerTransformAdapter());
		gb.registerTypeAdapter( new TypeToken< Map< String, DataSource > >(){}.getType(), new DataSourceMapAdapter());
		gb.disableHtmlEscaping();

		if ( prettyPrinting )
			gb.setPrettyPrinting();

		Gson gson = gb.create();
		return gson;
	}
}
