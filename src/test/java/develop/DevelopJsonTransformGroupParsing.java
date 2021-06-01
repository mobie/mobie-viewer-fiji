package develop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.xml.transform.Source;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DevelopJsonTransformGroupParsing
{
	public interface SourceTransformer
	{
		List< Source > transform( List< Source > sources );
	}

	public static class AffineSourceTransformer implements SourceTransformer
	{
		public String[] sources;
		public double[] transform;

		@Override
		public List< Source > transform( List< Source > sources )
		{
			return null;
		}
	}

	public static class AutoGridSourceTransformer implements SourceTransformer
	{
		public String[][] sources;
		public String gridType;

		@Override
		public List< Source > transform( List< Source > sources )
		{
			return null;}
	}

	public static class SourceTransformerSupplier
	{
		AffineSourceTransformer affine;
		AutoGridSourceTransformer autoGrid;

		public SourceTransformer get()
		{
			return null;
		}
	}

	public static class View
	{
		public List< SourceTransformerSupplier > transformGroups;
	}

	public static void main( String[] args )
	{
		Gson gson = new Gson();

		Type type = new TypeToken< View >() {}.getType();
		View view = gson.fromJson( json(), type );

		final ArrayList< SourceTransformer > sourceTransformers = new ArrayList<>();
		for ( SourceTransformerSupplier transformerSupplier : view.transformGroups )
		{
			sourceTransformers.add( transformerSupplier.get() );
		}
	}

	public static String json()
	{
		return "{\n" +
				"  \"transformGroups\": [\n" +
				"    {\"affine\": {\"transform\" : [0,0,0], \"sources\": [\"imE\", \"imF\"]}},\n" +
				"    {\"affine\": {\"transform\" : [1,0,0], \"sources\": [\"imG\", \"imH\"]}},\n" +
				"    {\"autoGrid\": {\"gridType\" : \"something\", \"sources\": [ [\"imA\", \"imB\"], [\"imC\", \"imD\"] ]}}\n" +
				"  ]\n" +
				"}";
	}
}
