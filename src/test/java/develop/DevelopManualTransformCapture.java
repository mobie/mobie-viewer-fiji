package develop;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.List;

public class DevelopManualTransformCapture
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData spimData = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.xml" );

		final BdvStackSource< ? > bdvStackSource = BdvFunctions.show( spimData ).get( 0 );
		final TransformedSource< ? > transformedSource = (TransformedSource) bdvStackSource.getSources().get( 0 ).getSpimSource();

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvStackSource.getBdvHandle().getTriggerbindings(), "behaviours" );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
				final AffineTransform3D transform3D = new AffineTransform3D();
				transformedSource.getSourceTransform( 0, 0, transform3D );
				System.out.println( "Transform: " + transform3D );
		}, "Log transform", "L"  ) ;
	}
}
