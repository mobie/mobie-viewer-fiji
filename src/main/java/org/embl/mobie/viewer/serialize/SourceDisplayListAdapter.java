package org.embl.mobie.viewer.serialize;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.display.ImageDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.transform.SourceTransformer;

import java.lang.reflect.Type;
import java.util.*;

public class SourceDisplayListAdapter implements JsonSerializer< List< SourceDisplay > >, JsonDeserializer< List< SourceDisplay > >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("imageDisplay", ImageDisplay.class);
		classToName.put( ImageDisplay.class.getName(), "imageDisplay");
		nameToClass.put("segmentationDisplay", SegmentationDisplay.class);
		classToName.put( SegmentationDisplay.class.getName(), "segmentationDisplay");
		nameToClass.put("sourceAnnotationDisplay", RegionDisplay.class);
		classToName.put( RegionDisplay.class.getName(), "sourceAnnotationDisplay");
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

			if ( sourceDisplay instanceof ImageDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay, new TypeToken< Map< String, ImageDisplay > >() {}.getType() ) );
			} else if ( sourceDisplay instanceof SegmentationDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay , new TypeToken< Map< String, SegmentationDisplay > >() {}.getType() ) );
			} else if ( sourceDisplay instanceof RegionDisplay ) {
				ja.add( context.serialize( nameToSourceDisplay, new TypeToken< Map< String, RegionDisplay > >() {}.getType() ) );
			} else
			{
				throw new UnsupportedOperationException( "Could not serialise SourceDisplay of type: " + sourceDisplay.getClass().toString() );
			}
		}

		return ja;
	}
}
