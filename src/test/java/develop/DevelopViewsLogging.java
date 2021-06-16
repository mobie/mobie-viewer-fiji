package develop;

import com.google.gson.Gson;
import de.embl.cba.mobie.serialize.JsonHelper;
import de.embl.cba.mobie.transform.NormalizedAffineViewerTransform;
import de.embl.cba.mobie.transform.ViewerTransform;

public class DevelopViewsLogging
{
	public static void main( String[] args )
	{
		final NormalizedAffineViewerTransform affineViewerTransform = new NormalizedAffineViewerTransform( new double[ 12 ] );

		final Gson gson = JsonHelper.buildGson( false );
		final String json = gson.toJson( affineViewerTransform );
		System.out.printf( json );

		final ViewerTransform viewerTransform = gson.fromJson( json, ViewerTransform.class );
	}
}
