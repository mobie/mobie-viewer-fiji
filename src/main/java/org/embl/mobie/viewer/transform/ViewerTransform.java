package org.embl.mobie.viewer.transform;

import com.google.gson.Gson;
import org.embl.mobie.viewer.serialize.JsonHelper;

public interface ViewerTransform
{
	double[] getParameters();
	Integer getTimepoint();

	static ViewerTransform toViewerTransform( String s )
	{
		try
		{
			final Gson gson = JsonHelper.buildGson( false );
			return gson.fromJson( s, ViewerTransform.class );

		}
		catch ( Exception gsonException )
		{
			throw gsonException;
			//return new PositionViewerTransform(new double[]{0,0,0}, 0);
		}
	}

}
