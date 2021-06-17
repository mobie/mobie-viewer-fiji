package de.embl.cba.mobie.serialize;

import com.google.gson.*;
import de.embl.cba.mobie.transform.AffineViewerTransform;
import de.embl.cba.mobie.transform.NormalizedAffineViewerTransform;
import de.embl.cba.mobie.transform.PositionViewerTransform;
import de.embl.cba.mobie.transform.TimepointViewerTransform;
import de.embl.cba.mobie.transform.ViewerTransform;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

public class ViewerTransformAdapter implements JsonSerializer< ViewerTransform >, JsonDeserializer< ViewerTransform >
{
	private static Map<String, Class> nameToClass = new TreeMap<>();
	private static Map<String, String> classToName = new TreeMap<>();

	static {
		nameToClass.put("affine", AffineViewerTransform.class);
		classToName.put(AffineViewerTransform.class.getName(), "affine");
		nameToClass.put("normalizedAffine", NormalizedAffineViewerTransform.class);
		classToName.put(NormalizedAffineViewerTransform.class.getName(), "normalizedAffine");
		nameToClass.put("position", PositionViewerTransform.class);
		classToName.put(PositionViewerTransform.class.getName(), "position");
		nameToClass.put("timepoint", TimepointViewerTransform.class);
		classToName.put(TimepointViewerTransform.class.getName(), "timepoint");
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
