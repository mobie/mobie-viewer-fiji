package users.pao;

import bdv.SpimSource;
import bdv.viewer.Interpolation;
import itc.utilities.CopyUtils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImageJ;
import net.imglib2.*;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;

import itc.utilities.IntervalUtils;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;

public class ExportRegisteredFibSemLabels
{
	public static < T extends RealType< T > & NativeType< T > > void main( String[] args ) throws SpimDataException
	{

		SpimData medData = new XmlIoSpimData().load( "/Users/tischer/Desktop/AChE-MED.xml" );

		final AffineTransform3D transform3D
				= medData.getViewRegistrations()
				.getViewRegistration( 0, 0 ).getModel();

		final int setupId = 0;
		final RandomAccessibleInterval< ? > image = medData.getSequenceDescription()
				.getImgLoader().getSetupImgLoader( setupId )
				.getImage( 0 );

		final VoxelDimensions voxelDimensions
				= medData.getSequenceDescription()
				.getViewSetupsOrdered().get( setupId ).getVoxelSize();

		final RealInterval realInterval
				= IntervalUtils.toCalibratedRealInterval( image, voxelDimensions );



		System.out.println( realInterval.toString() );

//		final SpimData parapodCellData = new XmlIoSpimData().load( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-segmented-ganglion-parapod-fib.xml" );

		final SpimData parapodCellData = new XmlIoSpimData().load(
				"/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-raw-parapod-fib-affine_g.xml");

		final SpimSource< T > parapodSource = new SpimSource< T >( parapodCellData, 0, "" );
		parapodSource.getVoxelDimensions();

		final int numMipmapLevels = parapodSource.getNumMipmapLevels();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			parapodSource.getSourceTransform( 0, level, affineTransform3D );
			System.out.println( affineTransform3D );
		}

		final int level = 3;
		final RealRandomAccessible< T > interpolatedSource
				= parapodSource.getInterpolatedSource( 0, level, Interpolation.NEARESTNEIGHBOR );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		parapodSource.getSourceTransform( 0, level, affineTransform3D );
		final RealRandomAccessible< T > transformed
				= RealViews.transform( interpolatedSource, affineTransform3D );
		printValue( transformed, new double[]{ 177, 62, 152 } );

		final RandomAccessible< T > raster = Views.raster( transformed );
		final RandomAccessibleInterval< T > transformedRasteredCropped =
				Views.interval( raster, Intervals.largestContainedInterval( realInterval ) );

		final RandomAccessibleInterval< T > slice =
				Views.hyperSlice( transformedRasteredCropped, 2, 152 );
		final RandomAccessibleInterval< T > copy =
				CopyUtils.copyPlanarRaiMultiThreaded( slice, 4 );
		new ImageJ().ui().showUI();
		ImageJFunctions.show( copy, "" );

//
//		final Interval interval = Intervals.largestContainedInterval( realInterval );
//		BdvFunctions.show( interpolatedSource,
//				interval,
//				"" ).setDisplayRange( 0, 255 );
//
//		BdvFunctions.show( parapodSource ).setDisplayRange( 0, 255 );

//		final RandomAccessible transformedRA =
//				Transforms.createTransformedRaView(
//						images.get( i ),
//						transforms.get( i ),
//						new ClampingNLinearInterpolatorFactory() );
//
//		final FinalInterval transformedInterval =
//				Transforms.createBoundingIntervalAfterTransformation(
//						images.get( i ), transforms.get( i ) );
//
//		transformed.add( Views.interval( transformedRA, transformedInterval ) );

	}

	public static < T extends NumericType< T > > void printValue(
			RealRandomAccessible< T > interpolatedSource, double[] position )
	{
		final RealRandomAccess< T > realRandomAccess = interpolatedSource.realRandomAccess();
		realRandomAccess.setPosition( position );
		System.out.println( realRandomAccess.get() );
	}
}
