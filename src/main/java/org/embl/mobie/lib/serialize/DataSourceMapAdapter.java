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

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;

public class DataSourceMapAdapter implements JsonSerializer< Map< String, DataSource > >, JsonDeserializer< Map< String, DataSource > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("image", ImageDataSource.class);
		classToName.put( ImageDataSource.class.getName(), "image");
		nameToClass.put("segmentation", SegmentationDataSource.class);
		classToName.put( SegmentationDataSource.class.getName(), "segmentation");
		nameToClass.put("regions", RegionTableSource.class);
		classToName.put( RegionTableSource.class.getName(), "regions");
		nameToClass.put("spots", SpotDataSource.class);
		classToName.put( SpotDataSource.class.getName(), "spots");
	}

	@Override
	public Map< String, DataSource > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		final HashMap< String, DataSource > map = new HashMap<>();
		final JsonObject jo = json.getAsJsonObject();
		for ( Map.Entry<String, JsonElement> entry : jo.entrySet() )
		{
			final DataSource dataSource = ( DataSource ) JsonHelper.createObjectFromJsonValue( context, entry.getValue(), nameToClass );
			dataSource.setName( entry.getKey() );
			map.put( entry.getKey(), dataSource );
		}
		return map;
	}

	@Override
	public JsonElement serialize( Map< String, DataSource > sources, Type type, JsonSerializationContext context ) {

		JsonObject jo = new JsonObject();
		for ( Map.Entry<String, DataSource> entry : sources.entrySet() ) {

			Map<String, DataSource> typeOfSourceToSource = new HashMap<>();
			typeOfSourceToSource.put( classToName.get( entry.getValue().getClass().getName() ), entry.getValue() );

			jo.add( entry.getKey(), context.serialize( typeOfSourceToSource ) );
		}

		return jo;
	}
}
