package develop;

import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class ExploreJsonLoadingFromGitlab2
{
	public static void main( String[] args ) throws IOException
	{
		URL url = new URL("https://git.embl.de/tischer/platy-browser-tables/raw/master/data/versions.json");

		final InputStream is = url.openStream();
		final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

		GsonBuilder builder = new GsonBuilder();
		Object o = builder.create().fromJson(reader, Object.class);
		int a = 1;
	}
}
