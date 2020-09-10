package develop;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale3D;

import java.util.stream.DoubleStream;

import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowCenter;
import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowHeight;

public class DevelopNormalisedViewerTransforms
{
	public static void main( String[] args )
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer( "https://github.com/mobie-org/covid-em-datasets" );
		//BdvUtils.moveToPosition( moBIEViewer.getSourcesPanel().getBdv(), new double[]{10,10,10}, 0, 500 );
		final String s = getBdvNormalisedViewerTransformString( moBIEViewer.getSourcesPanel().getBdv() );
		System.out.println( "Normalised transform");
		System.out.println( s );
	}

	public static String getBdvNormalisedViewerTransformString( BdvHandle bdv )
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdv.getViewerPanel().state().getViewerTransform( view );

		final AffineTransform3D translateWindowCenterToWindowOrigin = new AffineTransform3D();
		translateWindowCenterToWindowOrigin.translate( getBdvWindowCenter( bdv ) );
		view.preConcatenate( translateWindowCenterToWindowOrigin );

		final Scale3D scale = new Scale3D( 1.0 / BdvUtils.getBdvWindowWidth( bdv ) );
		view.preConcatenate( scale );

		return view.toString().replace( "3d-affine: (", "" ).replace( ")", "" );
	}


}
