package users.pao;

import bdv.SpimSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.io.BdvRaiVolumeExport;
import de.embl.cba.bdv.utils.io.BdvRaiXYZCTExport;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.transforms.utils.Transforms;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import itc.utilities.CopyUtils;
import itc.utilities.VectorUtils;
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
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.renjin.gnur.api.S;

import java.util.ArrayList;
import java.util.Arrays;

public class ExportRegisteredFibSemLabels
{
	public static < T extends RealType< T > & NativeType< T > > void main( String[] args ) throws SpimDataException
	{

		SpimData targetSpimData = new XmlIoSpimData().load( "/Users/tischer/Desktop/AChE-MED.xml" );
		
		final int setupId = 0;
		final Interval targetInterval = targetSpimData.getSequenceDescription()
				.getImgLoader().getSetupImgLoader( setupId )
				.getImage( 0 );

		final String voxelUnit = "micrometer";

		final double[] targetVoxelSpacing = getTargetVoxelSpacing( targetSpimData, setupId );
		Utils.log( "Target voxel spacing [" + voxelUnit + "]: " + VectorUtils.toString( targetVoxelSpacing ) );


		Utils.log( "REMOVE: Target interval [voxel]: " + IntervalUtils.toString( targetInterval )  );

		final RealInterval targetRealInterval
				= IntervalUtils.toCalibratedRealInterval( targetInterval, targetVoxelSpacing );

		Utils.log( "Target interval [" + voxelUnit + "]: " + IntervalUtils.toString( targetRealInterval ) );
		
		final SpimData spimData = new XmlIoSpimData().load(
				"/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-raw-parapod-fib-affine_g.xml");
		
		final SpimSource< T > spimSource = new SpimSource< T >( spimData, 0, "" );

		int level = getClosestSourceLevel( targetVoxelSpacing, spimData );
		
		final double[] sourceVoxelSpacing = BdvUtils.getVoxelSpacings( spimData, 0 ).get( level );

		Utils.log( "Source voxel spacing [" + voxelUnit + "]: " + VectorUtils.toString( sourceVoxelSpacing ) );

		//double[] scalingFactors = createScalingFactors( targetVoxelSpacing, sourceVoxelSpacing );

		final double[] scalingFactors = Arrays.stream( targetVoxelSpacing ).map( x -> 1.0 / x ).toArray();

		// Utils.log( "Voxel spacing scaling factors: " + VectorUtils.toString( scalingFactors ) );

		final Scale3D scale3D = new Scale3D( scalingFactors );

		final Interval transformedSourceInterval = Intervals.largestContainedInterval(
				IntervalUtils.scale( targetRealInterval, scalingFactors ) );

		Utils.log( "Transformed source interval [voxel]: " + IntervalUtils.toString( transformedSourceInterval ) );

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		spimSource.getSourceTransform( 0, level, sourceTransform );
		sourceTransform.preConcatenate( scale3D );

		final RealRandomAccessible< T > interpolatedSource
				= spimSource.getInterpolatedSource( 0, level, Interpolation.NEARESTNEIGHBOR );

		final RealRandomAccessible< T > transformed
				= RealViews.transform( interpolatedSource, sourceTransform );

		final RandomAccessible< T > transformedRastered = Views.raster( transformed );

		final RandomAccessibleInterval< T > transformedRasteredInterval =
				Views.interval( transformedRastered, transformedSourceInterval );

//		final RandomAccessibleInterval< T > slice =
//				Views.hyperSlice( transformedRasteredInterval, 2, (int)( 152 * scalingFactors[ 2 ] ) );

		Utils.log( "Creating target image..." );
		final RandomAccessibleInterval< T > copy =
				CopyUtils.copyVolumeRaiMultiThreaded( transformedRasteredInterval, 4 );

		Utils.log( "Saving as Tiff..." );
		final ImagePlus imagePlus = asImagePlus( copy, "", voxelUnit, targetVoxelSpacing );
		new FileSaver( imagePlus ).saveAsTiff( "/Users/tischer/Desktop/test.tif" );

		Utils.log( "Saving as Bdv..." );
		final RandomAccessibleInterval< T > raiXYZCT =
				Views.addDimension(
						Views.addDimension( copy, 0, 0 ),
						0, 0 );

		new BdvRaiXYZCTExport< T >().export(
				raiXYZCT,
				voxelUnit,
				"/Users/tischer/Desktop/test",
				targetVoxelSpacing,
				voxelUnit,
				new double[]{0,0,0});

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

	public static ImagePlus asImagePlus( RandomAccessibleInterval raiXYZ,
										 String title,
										 String voxelUnit,
										 double[] voxelSpacing )
	{
		final ImagePlus imp = ImageJFunctions.wrap(
				Views.permute(
						Views.addDimension( raiXYZ, 0, 0 ),
						2, 3 ), title );

		final Calibration calibration = new Calibration();
		calibration.pixelWidth = voxelSpacing[ 0 ];
		calibration.pixelHeight = voxelSpacing[ 1 ];
		calibration.pixelDepth = voxelSpacing[ 2 ];
		calibration.setUnit( voxelUnit );

		imp.setCalibration( calibration );

		return imp;
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

	public static double[] createScalingFactors( double[] targetVoxelSpacing,
												 double[] sourceVoxelSpacing )
	{

		final double[] scales = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
			scales[ d ] = targetVoxelSpacing[ d ] / sourceVoxelSpacing[ d ];

		return scales;
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

}
