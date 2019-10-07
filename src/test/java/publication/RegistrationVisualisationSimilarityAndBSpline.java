package publication;

import bdv.util.*;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.capture.BdvViewCaptures;
import de.embl.cba.platynereis.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

public class RegistrationVisualisationSimilarityAndBSpline< R extends RealType< R > & NativeType< R > >
{
	public static final ARGBType MAGENTA = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );
	public static final ARGBType GREEN = new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) );
	public static final ARGBType WHITE = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );

	public static final ARGBType RED = new ARGBType( ARGBType.rgba( 255, 0, 0, 255 ) );

	public static void main( String[] args )
	{
		Prefs.showScaleBar( true );
		Prefs.showMultibox( false );
		Prefs.scaleBarColor( ARGBType.rgba( 255, 255, 255, 255 ) );

		final AffineTransform3D gutViewSimilarity = new AffineTransform3D();
		gutViewSimilarity.set( new double[]{ 0.9476498880169756, -1.1429278186384646, -0.02353485655382334, 437.3256833175399, 1.1383751813599376, 0.940678083877728, 0.15525748717898924, -335.20810430942925, -0.10459366402618271, -0.11712788841143265, 1.476556609666939, -255.5571396760662 } );

		final AffineTransform3D ventralView = new AffineTransform3D();
		ventralView.set( new double[]{
				0.7640026486618857, 0.21675540046683806, -1.6962790277984714, 703.9725109035188, -1.1308673387472277, 1.4576715303334729, -0.32307624913149924, 500.1141846847264, 1.2827644000108667, 1.1559655258555834, 0.7254685989352216, -816.2576698664607 } );

		final AffineTransform3D gutViewBSpline = new AffineTransform3D();
		gutViewBSpline.set( new double[]{
				1.0424148768186736, -1.2572206005023119, -0.025888342209205707, 441.0582516492939, 1.2522126994959317, 1.0347458922655013, 0.17078323589688818, -397.62891474037247, -0.1150530304288011, -0.12884067725257614, 1.6242122706336333, -270.1128536436731 } );

		final RegistrationVisualisationSimilarityAndBSpline rvs = new RegistrationVisualisationSimilarityAndBSpline();


		/**
		 * Similarity
		 */

		final ImagePlus em = IJ.openImage( RegistrationVisualisationSimilarityAndBSpline.class.getResource( "../publication/em-onlyDAPI.zip" ).getFile() );

		final AffineTransform3D emTransform = new AffineTransform3D();
		final double[] doubles = Utils.delimitedStringToDoubleArray( "0.9975084647027952 0.0039003275874579927 -0.07043898275091061 10.291508243486646 -0.009467829800504684 0.9968391414786343 -0.07888020166688292 13.177618156352992 0.06990867646538076 0.07935057316018584 0.9943924092097696 -36.441354612353564", " " );
		emTransform.set( doubles );

		// Remove manual EM Transform for side view
		emTransform.set( new AffineTransform3D() );

		final BdvStackSource< ? > emStackSourceManuallyTransformed = BdvFunctions.show(
				rvs.getChannel( em, 0 ),
				"em",
				BdvOptions.options().sourceTransform( emTransform )
		);
		emStackSourceManuallyTransformed.setDisplayRange( 0, 255 );
		emStackSourceManuallyTransformed.setColor( MAGENTA );

		BdvHandle bdv = emStackSourceManuallyTransformed.getBdvHandle();

		addPositionAndViewLogging( bdv );

		final ImagePlus prosprSimilarity = IJ.openImage( RegistrationVisualisationSimilarityAndBSpline.class.getResource( "../publication/prospr-similarity.zip" ).getFile() );

		final BdvStackSource< ? > prosprSimilarityStackSource = BdvFunctions.show( rvs.getChannel( prosprSimilarity, 2 ), "prospr-similarity", BdvOptions.options().addTo( bdv ) );
		prosprSimilarityStackSource.setDisplayRange( 0, 255 );
		prosprSimilarityStackSource.setColor( GREEN );

		bdv.getViewerPanel().setCurrentViewerTransform( gutViewSimilarity );

		bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		bdv.getViewerPanel().setCurrentViewerTransform( ventralView );

		BdvViewCaptures.captureView( bdv, 1, "micrometer", false  );

		IJ.saveAs( "tif", "/Users/tischer/Desktop/similarity-side-view.tif" );

		/**
		 * BSpline
		 */


		final BdvStackSource< ? > emStackSource = BdvFunctions.show(
				rvs.getChannel( em, 0 ),
				"em" );
		emStackSource.setDisplayRange( 0, 255 );
		emStackSource.setColor( MAGENTA );

		bdv = emStackSource.getBdvHandle();

		addPositionAndViewLogging( bdv );

		final ImagePlus prosprBSpline = IJ.openImage( RegistrationVisualisationSimilarityAndBSpline.class.getResource( "../publication/prospr-bspline10.zip" ).getFile() );

		final BdvStackSource< ? > prosprBSplineStackSource = BdvFunctions.show( rvs.getChannel( prosprBSpline, 2 ), "prospr-bspline", BdvOptions.options().addTo( bdv ) );
		prosprBSplineStackSource.setDisplayRange( 0, 255 );
		prosprBSplineStackSource.setColor( GREEN );

		bdv.getViewerPanel().setCurrentViewerTransform( gutViewBSpline );
		bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		bdv.getViewerPanel().setCurrentViewerTransform( ventralView );

		BdvViewCaptures.captureView( bdv, 1, "micrometer", false  );

		IJ.saveAs( "tif", "/Users/tischer/Desktop/bspline-side-view.tif" );

	}


	public static void addPositionAndViewLogging( BdvHandle bdv )
	{
		Behaviours bSplineBehaviours = new Behaviours( new InputTriggerConfig() );
		bSplineBehaviours.install( bdv.getTriggerbindings(), "behaviours" );
		addPositionAndViewLoggingBehaviour( bdv, bSplineBehaviours );
	}

	public RandomAccessibleInterval< R >
	getChannel( ImagePlus em, int channel )
	{
		final RandomAccessibleInterval< R > emXYZCT = RegistrationVisualisationSimilarityAndBSpline.wrapXYZCT( em );

		return Views.hyperSlice( Views.hyperSlice( emXYZCT, 4, 0 ), 3, channel );
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > wrapXYZCT( ImagePlus imagePlus )
	{
		RandomAccessibleInterval< R > wrap = ImageJFunctions.wrapRealNative( imagePlus );

		if ( imagePlus.getNFrames() == 1 )
			wrap = Views.addDimension( wrap, 0, 0 );

		if ( imagePlus.getNChannels() == 1 )
		{
			wrap = Views.addDimension( wrap, 0, 0 );

			wrap = Views.permute(
					wrap,
					wrap.numDimensions() - 1,
					wrap.numDimensions() - 2 );
		}

		if ( imagePlus.getNSlices() == 1 )
		{
			wrap = Views.addDimension( wrap, 0, 0 );

			wrap = Views.permute(
					wrap,
					wrap.numDimensions() - 1,
					wrap.numDimensions() - 2 );
			wrap = Views.permute(
					wrap,
					wrap.numDimensions() - 2,
					wrap.numDimensions() - 3 );
		}
		else if ( imagePlus.getNSlices() > 1  && imagePlus.getNChannels() > 1 )
		{
			wrap = Views.permute(
					wrap,
					wrap.numDimensions() - 2,
					wrap.numDimensions() - 3 );
		}

		return wrap;
	}

	public static void addPositionAndViewLoggingBehaviour( BdvHandle bdv, org.scijava.ui.behaviour.util.Behaviours behaviours )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );
				Logger.log( "\nBigDataViewer position: \n" + globalMouseCoordinates.toString() );
				Logger.log( "BigDataViewer transform: \n"+ getBdvViewerTransform( bdv ) );
			} )).start();

		}, "Print position and view", "P"  ) ;

	}

	public static String getBdvViewerTransform( BdvHandle bdv )
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdv.getViewerPanel().getState().getViewerTransform( view );

		return view.toString().replace( "3d-affine", "View" );
	}

}
