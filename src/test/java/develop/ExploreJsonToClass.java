package develop;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.util.ArrayList;

public class ExploreJsonToClass
{

	public class Test
	{
		public ArrayList< String > datasets;
		public String defaultDataset;
	}

	public static void main( String[] args )
	{
		String json = "{\n" +
				"  \"datasets\": [\n" +
				"    \"10spd\",\n" +
				"    \"10spdbaf\"\n" +
				"  ],\n" +
				"  \"defaultDataset\": \"10spdbaf\"\n" +
				"}";

		Gson g = new Gson();
		Test p = (Test) g.fromJson(json, Test.class);
		final ArrayList< String > datasets = p.datasets;
	}
}
