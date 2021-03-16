package develop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.scene.transform.Affine;

import javax.xml.transform.Source;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DevelopJsonViewsParsing
{
	public interface SourcesTransformer
	{
		List< Source > transform( List< Source > sources );
	}

	public static class AffineSourcesTransformer implements SourcesTransformer
	{
		public String[] sources;
		public double[] transform;

		@Override
		public List< Source > transform( List< Source > sources )
		{
			return null;
		}
	}

	public static class AutoGridSourcesTransformer implements SourcesTransformer
	{
		public String[][] sources;
		public String gridType;

		@Override
		public List< Source > transform( List< Source > sources )
		{
			return null;
		}
	}

	public static class SourcesTransformerProvider
	{
		// This is ugly because we have to manually update it and all but one are null
		public AffineSourcesTransformer affine;
		public AutoGridSourcesTransformer autoGrid;

		public SourcesTransformer get()
		{
			if ( affine != null ) return affine;
			else if ( autoGrid != null ) return autoGrid;
			else return null;
		}
	}

	public static class TransformGroups
	{
		public List< SourcesTransformerProvider > transformGroups;
	}

	public static void main( String[] args )
	{
		Gson gson = new Gson();

		Type type = new TypeToken< TransformGroups >() {}.getType();
		TransformGroups transformGroups = gson.fromJson( json(), type );
		transformGroups.transformGroups.get( 0 ).get().transform( null );
	}

	public static String json()
	{
		return "{\n" +
				"  \"transformGroups\": [\n" +
				"    {\"affine\": {\"transform\" : [0,0,0], \"sources\": [\"imE\", \"imF\"]}},\n" +
				"    {\"affine\": {\"transform\" : [1,0,0], \"sources\": [\"imG\", \"imH\"]}},\n" +
				"    {\"autoGrid\": {\"gridType\" : \"something\", \"sources\": [ [\"imA\", \"imB\"], [\"imC\", \"imD\"] ]}},\n" +
				"  ]\n" +
				"}";
	}
}
