package publication;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.platynereis.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

public class RegistrationVisualisationSimilarity < R extends RealType< R > & NativeType< R > >
{
	public static final ARGBType MAGENTA = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );

	public static < R extends RealType< R > & NativeType< R > > void main( String[] args )
	{
		final RegistrationVisualisationSimilarity rvs = new RegistrationVisualisationSimilarity();

		final ImagePlus em = IJ.openImage( RegistrationVisualisationSimilarity.class.getResource( "../publication/em-onlyDAPI.zip" ).getFile() );

		final AffineTransform3D emTransform = new AffineTransform3D();

		final double[] doubles = Utils.delimitedStringToDoubleArray( "0.9975084647027952 0.0039003275874579927 -0.07043898275091061 10.291508243486646 -0.009467829800504684 0.9968391414786343 -0.07888020166688292 13.177618156352992 0.06990867646538076 0.07935057316018584 0.9943924092097696 -36.441354612353564", " " );

		emTransform.set( doubles );

		final BdvStackSource< R > emStackSource = BdvFunctions.show(
				rvs.getChannel( em, 0 ),
				"em",
				BdvOptions.options().sourceTransform( emTransform )
				);
		emStackSource.setDisplayRange( 0, 255 );
		emStackSource.setColor( MAGENTA );

		final BdvHandle bdv = emStackSource.getBdvHandle();

		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getTriggerbindings(), "behaviours" );

		addPositionAndViewLoggingBehaviour( bdv, behaviours );

		final ImagePlus prospr = IJ.openImage( RegistrationVisualisationSimilarity.class.getResource( "../publication/prospr-similarity.zip" ).getFile() );

		final BdvStackSource< R > prosprSimilarityStackSource = BdvFunctions.show( rvs.getChannel( prospr, 2 ), "prospr", BdvOptions.options().addTo( bdv ) );
		prosprSimilarityStackSource.setDisplayRange( 0, 255 );
		prosprSimilarityStackSource.setColor( new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) ) );
	}

	public RandomAccessibleInterval< R >
	getChannel( ImagePlus em, int channel )
	{
		final RandomAccessibleInterval< R > emXYZCT = RegistrationVisualisationSimilarity.wrapXYZCT( em );

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
