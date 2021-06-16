package develop;

import com.google.gson.Gson;
import de.embl.cba.mobie.transform.AffineViewerTransform;
import de.embl.cba.mobie.transform.BdvLocationSupplier;
import de.embl.cba.mobie.transform.BdvLocationType;

public class DevelopViewsLogging
{
	public static void main( String[] args )
	{
		final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( BdvLocationType.Position3d, new double[ 3 ] );
		final BdvLocationSupplier supplier = new BdvLocationSupplier( affineViewerTransform );

		final Gson gson = new Gson();
		final String json = gson.toJson( supplier );
		System.out.printf( json );
		final BdvLocationSupplier bdvLocationSupplier = gson.fromJson( json, BdvLocationSupplier.class );
	}
}
