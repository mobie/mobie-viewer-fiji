package de.embl.cba.mobie2.serialize;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.tables.FileAndUrlUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class JsonHelper
{
	public static double[] fetchLeveling( String dataLocation )
	{
		final String levelingFile = FileAndUrlUtils.combinePath( dataLocation, "misc/leveling.json" );
		try
		{
			InputStream is = FileAndUrlUtils.getInputStream( levelingFile );
			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
			final GsonBuilder gsonBuilder = new GsonBuilder();
			LinkedTreeMap linkedTreeMap = gsonBuilder.create().fromJson( reader, Object.class );
			ArrayList< Double > normalVector = ( ArrayList< Double > ) linkedTreeMap.get( "NormalVector" );
			final double[] doubles = normalVector.stream().mapToDouble( Double::doubleValue ).toArray();
			return doubles;
		}
		catch ( Exception e )
		{
			return null; // new double[]{0.70,0.56,0.43};
		}
	}
}
