package explore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExploreJsonToLayersMap
{
	public static class LayerProperty
	{
		public double maxValue = 100;
		public double minValue = 0;
	}

	public static void main( String[] args )
	{
		final HashMap< String, LayerProperty > map = new HashMap<>();

		final LayerProperty layerProperty00 = new LayerProperty();
		final LayerProperty layerProperty01 = new LayerProperty();

		map.put( "imA", layerProperty00 );
		map.put( "imB", layerProperty01 );

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String json = gson.toJson( map );
		System.out.println( json );

		Type empMapType = new TypeToken< Map<String, LayerProperty> >() {}.getType();
		final Object o = gson.fromJson( json, empMapType );
	}
}
