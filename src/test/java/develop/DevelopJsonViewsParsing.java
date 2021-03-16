package develop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DevelopJsonViewsParsing
{
	public static class Affine
	{
		public double[] transform;
	}

	public static class TransformGroups
	{
		// public ArrayList< Affine > transformGroups;
		public ArrayList<Map<String, Affine>> transformGroups;
	}

	public static void main( String[] args )
	{
		Gson gson = new Gson();

		// forward serialization
		/*
		TransformGroups tg = new TransformGroups();

		Map<String, Affine> trafo = new HashMap<String, Affine>();
		Affine affine = new Affine();
		double[] params = {1., 0., 0.};
		affine.transform = params;
		trafo.put("affine", affine);

		ArrayList<Map<String, Affine>> trafos = new ArrayList<Map<String, Affine>>();
		trafos.add(trafo);

		tg.transformGroups = trafos;

		String serialized = gson.toJson(tg);
		System.out.println(serialized);
		 */

		// backward deserialization
		Type type = new TypeToken< TransformGroups >() {}.getType();
		TransformGroups tg = gson.fromJson( json0(), type );
		System.out.println(tg.transformGroups.get(0).get("affine").transform);
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
