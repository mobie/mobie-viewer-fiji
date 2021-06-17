package de.embl.cba.mobie.serialize;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie.display.ImageSourceDisplay;
import de.embl.cba.mobie.display.SegmentationSourceDisplay;
import de.embl.cba.mobie.display.SourceDisplay;
import de.embl.cba.mobie.transform.SourceTransformer;

import java.lang.reflect.Type;
import java.util.*;

public class SourceDisplayListAdapter implements JsonSerializer< List< SourceDisplay > >, JsonDeserializer< List< SourceDisplay > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("imageDisplay", ImageSourceDisplay.class);
		classToName.put(ImageSourceDisplay.class.getName(), "imageDisplay");
		nameToClass.put("segmentationDisplay", SegmentationSourceDisplay.class);
		classToName.put(SegmentationSourceDisplay.class.getName(), "segmentationDisplay");
	}

	@Override
	public List< SourceDisplay > deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
	{
		List list = new ArrayList<SourceTransformer>();
		JsonArray ja = json.getAsJsonArray();

		for (JsonElement je : ja)
		{
			list.add( JsonHelper.createObjectFromJsonValue( context, je, nameToClass ) );
		}

		return list;
	}

	@Override
	public JsonElement serialize( List< SourceDisplay > sourceDisplays, Type type, JsonSerializationContext context ) {
		JsonArray ja = new JsonArray();
		for ( SourceDisplay sourceDisplay: sourceDisplays ) {
			Map< String, SourceDisplay > nameToSourceDisplay = new HashMap<>();
			nameToSourceDisplay.put( classToName.get( sourceDisplay.getClass().getName() ), sourceDisplay );

			if ( sourceDisplay instanceof ImageSourceDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay, new TypeToken< Map< String, ImageSourceDisplay > >() {}.getType() ) );
			} else if ( sourceDisplay instanceof  SegmentationSourceDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay , new TypeToken< Map< String, SegmentationSourceDisplay > >() {}.getType() ) );
			} else {
				throw new UnsupportedOperationException( "Could not serialise SourceDisplay of type: " + sourceDisplay.getClass().toString() );
			}
		}

		return ja;
	}
}
