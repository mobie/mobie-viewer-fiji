package users.sultan;

import bdv.tools.transformation.TransformedSource;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.List;

public class ViewNeuropils
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData platyBrowserNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.xml" );
		SpimData xRayNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um.xml" );


		final List< BdvStackSource< ? > > show = BdvFunctions.show( platyBrowserNeuropil );
		final Source< ? > spimSource = show.get( 0 ).getSources().get( 0 ).getSpimSource();
		BdvFunctions.show( xRayNeuropil, BdvOptions.options().addTo( show.get( 0 ) ) );

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( show.get( 0 ).getBdvHandle().getTriggerbindings(), "behaviours" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final TransformedSource transformedSource = ( TransformedSource ) spimSource;
				final AffineTransform3D fixed = new AffineTransform3D();
				transformedSource.getFixedTransform( fixed );
				System.out.println( "Fixed: " + fixed.toString() );

				final AffineTransform3D incr = new AffineTransform3D();
				transformedSource.getIncrementalTransform( incr );
				System.out.println( "Incr: " + incr.toString() );

				final AffineTransform3D whole = new AffineTransform3D();
				transformedSource.getSourceTransform( 0, 0, whole );
				System.out.println( "Whole: " + whole.toString() );

			} )).start();

		}, "Print position and view", "P"  ) ;

	}
}
