package users.pao;

import bdv.SpimSource;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.utils.Utils;
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
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;

import java.util.ArrayList;

public class ExportRegisteredFibSemLabels
{
	public static < T extends RealType< T > & NativeType< T > > void main( String[] args ) throws SpimDataException
	{

		SpimData targetSpimData = new XmlIoSpimData().load( "/Users/tischer/Desktop/AChE-MED.xml" );
		
		final int setupId = 0;
		final RandomAccessibleInterval< ? > targetRai = targetSpimData.getSequenceDescription()
				.getImgLoader().getSetupImgLoader( setupId )
				.getImage( 0 );
		
		final double[] targetVoxelSpacing = getTargetVoxelSpacing( targetSpimData, setupId );
		Utils.log( "Target voxel spacing [unit]: " + LinAlgHelpers.toString( targetVoxelSpacing ) );

		Utils.log( "Target interval [voxel]: " + targetRai );

		final RealInterval targetRealInterval
				= IntervalUtils.toCalibratedRealInterval( targetRai, targetVoxelSpacing );
		Utils.log( "Target interval [unit]: " + targetRealInterval );
		
		final SpimData spimData = new XmlIoSpimData().load(
				"/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-raw-parapod-fib-affine_g.xml");
		
		final SpimSource< T > spimSource = new SpimSource< T >( spimData, 0, "" );

		int level = getClosestSourceLevel( targetVoxelSpacing, spimData );
		
		final double[] sourceVoxelSpacing = BdvUtils.getVoxelSpacings( spimData, 0 ).get( level );

		Utils.log( "Source voxel spacing [unit]: " + LinAlgHelpers.toString( sourceVoxelSpacing ) );

		AffineTransform3D voxelSpacingTransform3D = getVoxelSpacingTransform3D( targetVoxelSpacing, sourceVoxelSpacing );

		voxelSpacingTransform3D.estimateBounds( targetRealInterval );

		Utils.log( "Transformed source interval [voxel]" );

		Utils.log( "");
		
		
		final Interval targetInterval = Intervals.largestContainedInterval(
				voxelSpacingTransform3D.estimateBounds( targetRealInterval ) );


		final AffineTransform3D sourceTransform = new AffineTransform3D();
		spimSource.getSourceTransform( 0, level, sourceTransform );
		sourceTransform.preConcatenate( voxelSpacingTransform3D );

		final RealRandomAccessible< T > interpolatedSource
				= spimSource.getInterpolatedSource( 0, level, Interpolation.NEARESTNEIGHBOR );

		final RealRandomAccessible< T > transformed
				= RealViews.transform( interpolatedSource, sourceTransform );

		final RandomAccessible< T > transformedRastered = Views.raster( transformed );

		final RandomAccessibleInterval< T > transformedRasteredInterval =
				Views.interval( transformedRastered, targetInterval );

		final RandomAccessibleInterval< T > slice =
				Views.hyperSlice( transformedRasteredInterval, 2, 152 );

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

	public static double[] getTargetVoxelSpacing( SpimData medData, int setupId )
	{
		final VoxelDimensions targetVoxelDimensions
				= medData.getSequenceDescription()
				.getViewSetupsOrdered().get( setupId ).getVoxelSize();

		final double[] targetVoxelSpacing = new double[ targetVoxelDimensions.numDimensions() ];
		for ( int d = 0; d < targetVoxelSpacing.length; d++ )
			targetVoxelSpacing[ d ] = targetVoxelDimensions.dimension( d );
		return targetVoxelSpacing;
	}

	public static AffineTransform3D getVoxelSpacingTransform3D( double[] targetVoxelSpacing,
																double[] sourceVoxelSpacing )
	{
		AffineTransform3D sourceToTargetVoxelSpacing = new AffineTransform3D();
		for ( int d = 0; d < 3; ++d )
			sourceToTargetVoxelSpacing.set(
					targetVoxelSpacing[ d ] / sourceVoxelSpacing[ d ], d, d );
		return sourceToTargetVoxelSpacing;
	}

	public static int getClosestSourceLevel( double[] targetVoxelSpacing, SpimData sourceData )
	{
		final ArrayList< double[] > voxelSpacings = BdvUtils.getVoxelSpacings( sourceData, 0 );

		int level = 0;
		for ( ; level < voxelSpacings.size(); level++ )
		{
			if ( voxelSpacings.get( level )[ 0 ] > targetVoxelSpacing[ 0 ] )
				break;
		}
		if ( level > 0 ) level -= 1;

		return level;
	}

	public static < T extends NumericType< T > > void printValue(
			RealRandomAccessible< T > interpolatedSource, double[] position )
	{
		final RealRandomAccess< T > realRandomAccess = interpolatedSource.realRandomAccess();
		realRandomAccess.setPosition( position );
		Utils.log( realRandomAccess.get() );
	}
}
