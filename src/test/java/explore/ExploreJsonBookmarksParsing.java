package explore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExploreJsonBookmarksParsing
{
	public static class Bookmark
	{
		public HashMap< String, ImageLayerProperties > layers;
		public double[] view;
	}

	public static class ImageLayerProperties
	{
		public double maxValue = 100;
		public double minValue = 0;
	}

	public static void main( String[] args )
	{
		String json = getJsonString();


		Gson gson = new Gson();
		Type type = new TypeToken< Map< String, Bookmark > >() {}.getType();
		final Map< String, Bookmark > bookmarks = gson.fromJson( json, type );
		final Bookmark bookmark = bookmarks.get( "default" );
	}

	public static String getJsonString()
	{
		return "";
	}


}
