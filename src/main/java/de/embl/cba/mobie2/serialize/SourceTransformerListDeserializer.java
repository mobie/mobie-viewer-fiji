package de.embl.cba.mobie2.serialize;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.embl.cba.mobie2.transform.AffineSourceTransformer;
import de.embl.cba.mobie2.transform.CropSourceTransformer;
import de.embl.cba.mobie2.transform.GridSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SourceTransformerListDeserializer implements JsonDeserializer< List< SourceTransformer > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();

	static {
		nameToClass.put("grid", GridSourceTransformer.class);
		nameToClass.put("affine", AffineSourceTransformer.class);
		nameToClass.put("crop", CropSourceTransformer.class);
	}

	@Override
	public List< SourceTransformer > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		List list = new ArrayList<SourceTransformer>();
		JsonArray ja = json.getAsJsonArray();

		for (JsonElement je : ja)
		{
			list.add( JsonHelper.getObject( context, je, nameToClass ));
		}

		return list;
	}
}
