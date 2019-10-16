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
		gutViewSimilarity.set( new double[]{ -0.9276381700057749, 1.1591708414422703, 0.02624088884985559, 358.60730633258726, -1.1547405723118895, -0.9205879732299214, -0.15482310082966486, 917.702862007929, -0.10459366402618271, -0.11712788841143265, 1.476556609666939, -253.55713967606619 } );

		final AffineTransform3D headViewSimilarity = new AffineTransform3D();
		headViewSimilarity.set( new double[] { -0.9673729426921943, 1.1263366489493118, 0.020821655312497484, 372.5739351026749, -1.1216630303000528, -0.9604816548686338, -0.1556445805937058, 962.76445738753, -0.10459366402618273, -0.11712788841143265, 1.476556609666939, -45.557139676066214
 } );

		final RegistrationVisualisationSimilarityAndBSpline rvs = new RegistrationVisualisationSimilarityAndBSpline();

		/**
		 * Similarity
		 */

		final ImagePlus em = IJ.openImage( RegistrationVisualisationSimilarityAndBSpline.class.getResource( "../publication/em-onlyDAPI.zip" ).getFile() );

		final AffineTransform3D emManualGutTransform = new AffineTransform3D();
		final double[] doubles = Utils.delimitedStringToDoubleArray( "0.9975084647027952 0.0039003275874579927 -0.07043898275091061 10.291508243486646 -0.009467829800504684 0.9968391414786343 -0.07888020166688292 13.177618156352992 0.06990867646538076 0.07935057316018584 0.9943924092097696 -36.441354612353564", " " );
		emManualGutTransform.set( doubles );

		//emManualGutTransform.set( new AffineTransform3D() );

		final BdvStackSource< ? > emStackSourceManuallyTransformed = BdvFunctions.show(
				rvs.getChannel( em, 0 ),
				"em",
				BdvOptions.options().sourceTransform( emManualGutTransform )
		);
		emStackSourceManuallyTransformed.setDisplayRange( 0, 255 );
		emStackSourceManuallyTransformed.setColor( MAGENTA );

		BdvHandle bdv = emStackSourceManuallyTransformed.getBdvHandle();

		addPositionAndViewLogging( bdv );

		final ImagePlus prosprSimilarity = IJ.openImage( RegistrationVisualisationSimilarityAndBSpline.class.getResource( "../publication/prospr-similarity.zip" ).getFile() );

		final BdvStackSource< ? > prosprSimilarityStackSource = BdvFunctions.show( rvs.getChannel( prosprSimilarity, 2 ), "prospr-similarity", BdvOptions.options().addTo( bdv ) );
		prosprSimilarityStackSource.setDisplayRange( 0, 255 );
		prosprSimilarityStackSource.setColor( GREEN );

		bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		bdv.getViewerPanel().setCurrentViewerTransform( headViewSimilarity );
		IJ.wait( 2000 );
		BdvViewCaptures.captureView( bdv, 1, "micrometer", false  );
		IJ.saveAs( "tif", "/Users/tischer/Desktop/similarity-head-view.tif" );

		bdv.getViewerPanel().setCurrentViewerTransform( gutViewSimilarity );
		IJ.wait( 2000 );
		BdvViewCaptures.captureView( bdv, 1, "micrometer", false  );
		IJ.saveAs( "tif", "/Users/tischer/Desktop/similarity-gut-view.tif" );

		/**
		 * BSpline
		 */

		boolean bSpline = true;
		if ( bSpline )
		{
			final BdvStackSource< ? > emStackSource = BdvFunctions.show(
					rvs.getChannel( em, 0 ),
					"em" );
			emStackSource.setDisplayRange( 0, 255 );
			emStackSource.setColor( MAGENTA );

			bdv = emStackSource.getBdvHandle();
			bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

			addPositionAndViewLogging( bdv );

			final ImagePlus prosprBSpline = IJ.openImage( RegistrationVisualisationSimilarityAndBSpline.class.getResource( "../publication/prospr-bspline10.zip" ).getFile() );

			final BdvStackSource< ? > prosprBSplineStackSource = BdvFunctions.show( rvs.getChannel( prosprBSpline, 2 ), "prospr-bspline", BdvOptions.options().addTo( bdv ) );
			prosprBSplineStackSource.setDisplayRange( 0, 255 );
			prosprBSplineStackSource.setColor( GREEN );

			bdv.getViewerPanel().setCurrentViewerTransform( headViewSimilarity );
			IJ.wait( 2000 );
			BdvViewCaptures.captureView( bdv, 1, "micrometer", false  );
			IJ.saveAs( "tif", "/Users/tischer/Desktop/bspline-head-view.tif" );

			bdv.getViewerPanel().setCurrentViewerTransform( gutViewSimilarity );
			IJ.wait( 2000 );
			BdvViewCaptures.captureView( bdv, 1, "micrometer", false  );
			IJ.saveAs( "tif", "/Users/tischer/Desktop/bspline-gut-view.tif" );
		}

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
