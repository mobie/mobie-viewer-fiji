package develop;

import com.google.gson.Gson;
import org.embl.mobie.viewer.serialize.JsonHelper;
import org.embl.mobie.viewer.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.viewer.transform.ViewerTransform;

public class DevelopViewsLogging
{
	public static void main( String[] args )
	{
		final NormalizedAffineViewerTransform affineViewerTransform = new NormalizedAffineViewerTransform( new double[ 12 ], 0 );

		final Gson gson = JsonHelper.buildGson( false );
		final String json = gson.toJson( affineViewerTransform );
		System.out.printf( json );

		final ViewerTransform viewerTransform = gson.fromJson( json, ViewerTransform.class );
	}
}
