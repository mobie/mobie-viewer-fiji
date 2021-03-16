package develop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class DevelopJsonViewsParsing
{
	public static class Affine
	{
		public double[] transform;
	}

	public static class TransformGroups
	{
		public ArrayList< Affine > transformGroups;
	}

	public static void main( String[] args )
	{
		Gson gson = new Gson();

		Type type = new TypeToken< TransformGroups >() {}.getType();
		TransformGroups transformGroups = gson.fromJson( json1(), type );
	}

	public static String json0()
	{
		return "{\n" +
				"  \"transformGroups\": [\n" +
				"    {\"affine\": {\"transform\": [0,0,0]}},\n" +
				"    {\"affine\": {\"transform\": [1,0,0]}}\n" +
				"  ]\n" +
				"}";
	}

	public static String json1()
	{
		return "{\n" +
				"  \"transformGroups\": [\n" +
				"    {\"transform\": [0,0,0]},\n" +
				"    {\"transform\": [1,0,0]}\n" +
				"  ]\n" +
				"}";
	}
}
