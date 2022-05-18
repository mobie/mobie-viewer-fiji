package org.embl.mobie.viewer.transform;

import com.google.gson.Gson;
import org.embl.mobie.viewer.serialize.JsonHelper;

public interface ViewerTransform
{
	double[] getParameters();

	Integer getTimepoint();

	static String toString( ViewerTransform viewerTransform )
	{
		if ( viewerTransform != null )
		{
			final Gson gson = JsonHelper.buildGson( false );
			return gson.toJson( viewerTransform );
		}
		else
			return  "";
	}

	static ViewerTransform toViewerTransform( String s )
	{
		try
		{
			final Gson gson = JsonHelper.buildGson( false );
			return gson.fromJson( s, ViewerTransform.class );

		}
		catch ( Exception gsonException )
		{
			// TODO: implement additional parsing: https://github.com/mobie/mobie-viewer-fiji/issues/731
			throw gsonException;
		}
	}

}
