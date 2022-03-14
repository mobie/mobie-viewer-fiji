package org.embl.mobie.viewer.serialize;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.transform.SourceTransformer;
import org.embl.mobie.viewer.transform.ViewerTransform;
import org.embl.mobie.io.util.FileAndUrlUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonHelper
{
	public static double[] fetchLeveling( String dataLocation )
	{
		final String levelingFile = FileAndUrlUtils.combinePath( dataLocation, "misc/leveling.json" );
		try
		{
			InputStream is = FileAndUrlUtils.getInputStream( levelingFile );
			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
			final GsonBuilder gsonBuilder = new GsonBuilder();
			LinkedTreeMap linkedTreeMap = gsonBuilder.create().fromJson( reader, Object.class );
			ArrayList< Double > normalVector = ( ArrayList< Double > ) linkedTreeMap.get( "NormalVector" );
			final double[] doubles = normalVector.stream().mapToDouble( Double::doubleValue ).toArray();
			return doubles;
		}
		catch ( Exception e )
		{
			return null; // new double[]{0.70,0.56,0.43};
		}
	}

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
		gb.registerTypeAdapter( new TypeToken<List<SourceTransformer>>(){}.getType(), new SourceTransformerListAdapter());
		gb.registerTypeAdapter( new TypeToken<List< SourceDisplay >>(){}.getType(), new SourceDisplayListAdapter());
		gb.registerTypeAdapter( new TypeToken<ViewerTransform>(){}.getType(), new ViewerTransformAdapter());
		//gb.registerTypeAdapter( new TypeToken< MobieBdvSupplier >(){}.getType(), new MobieBdvSupplierAdapter());
		gb.disableHtmlEscaping();

		if ( prettyPrinting ) {
			gb.setPrettyPrinting();
		}
		Gson gson = gb.create();
		return gson;
	}
}
