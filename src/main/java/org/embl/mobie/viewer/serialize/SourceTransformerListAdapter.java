package org.embl.mobie.viewer.serialize;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.viewer.transform.AffineSourceTransformer;
import org.embl.mobie.viewer.transform.CropSourceTransformer;
import org.embl.mobie.viewer.transform.TransformedGridSourceTransformer;
import org.embl.mobie.viewer.transform.GridSourceTransformer;
import org.embl.mobie.viewer.transform.SourceTransformer;

import java.lang.reflect.Type;
import java.util.*;

public class SourceTransformerListAdapter implements JsonSerializer< List<SourceTransformer> >, JsonDeserializer< List< SourceTransformer > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("grid", GridSourceTransformer.class);
		classToName.put( GridSourceTransformer.class.getName(), "grid");
		nameToClass.put("transformGrid", TransformedGridSourceTransformer.class);
		classToName.put( TransformedGridSourceTransformer.class.getName(), "transformGrid");
		nameToClass.put("affine", AffineSourceTransformer.class);
		classToName.put(AffineSourceTransformer.class.getName(), "affine");
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
			} else {
				throw new UnsupportedOperationException( "Could not serialise SourceTransformer of type: " + sourceTransformer.getClass().toString() );
			}
		}

		return ja;
	}
}
