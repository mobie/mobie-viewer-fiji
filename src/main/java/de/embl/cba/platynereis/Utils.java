package de.embl.cba.platynereis;

import bdv.util.*;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import de.embl.cba.bdv.utils.transformhandlers.BehaviourTransformEventHandler3DGoogleMouse;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.DoubleStream;

public class Utils
{

	public static double[] delimitedStringToDoubleArray(String s, String delimiter) {

		String[] sA = s.split(delimiter);
		double[] nums = new double[sA.length];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = Double.parseDouble(sA[i].trim());
		}

		return nums;
	}


	public static void zoomToInterval( FinalRealInterval interval, Bdv bdv )
	{
		final AffineTransform3D affineTransform3D = getImageZoomTransform( interval, bdv );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );
	}

	public static AffineTransform3D getImageZoomTransform( FinalRealInterval interval, Bdv bdv )
	{

		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];

		for( int d = 0; d < 3; ++d )
		{
			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			centerPosition[ d ] = - center;
		}

		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		final double intervalSize = interval.realMax( 0 ) - interval.realMin( 0 );
		affineTransform3D.scale(  1.0 * bdvWindowDimensions[ 0 ] / intervalSize );

		double[] shiftToBdvWindowCenter = new double[ 3 ];

		for( int d = 0; d < 2; ++d )
		{
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		}

		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	public static void centerBdvViewToPosition( double[] position, double scale, Bdv bdv )
	{
		final AffineTransform3D newViewerTransform = getNewViewerTransform( position, scale, bdv );

		final double cX = 0; //- bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
		final double cY = 0; //- bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator( currentViewerTransform, newViewerTransform, cX ,cY, 3000 );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		bdv.getBdvHandle().getViewerPanel().transformChanged( currentViewerTransform );
	}

	public static double[] getBdvWindowCenter( Bdv bdv )
	{
		final double[] centre = new double[ 3 ];

		centre[ 0 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
		centre[ 1 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

		return centre;
	}

	private static AffineTransform3D getNewViewerTransform( double[] position, double scale, Bdv bdv )
	{
		final AffineTransform3D newViewerTransform = new AffineTransform3D();

		int[] bdvWindowDimensions = new int[ 3 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		double[] translation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
			translation[ d ] = - position[ d ];
		}

		newViewerTransform.setTranslation( translation );
		newViewerTransform.scale( scale );

		double[] centerBdvWindowTranslation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
			centerBdvWindowTranslation[ d ] = + bdvWindowDimensions[ d ] / 2.0;
		}

		newViewerTransform.translate( centerBdvWindowTranslation );

		return newViewerTransform;
	}


	public static long[] asLongs( double[] doubles )
	{
		final long[] longs = new long[ doubles.length ];

		for ( int i = 0; i < doubles.length; ++i )
		{
			longs[ i ] = (long) doubles[ i ];
		}

		return longs;
	}

	public static void log( String text )
	{
		IJ.log( text );
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V> > list = new ArrayList<>(map.entrySet());
		list.sort( Map.Entry.comparingByValue() );

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	public static < T extends RealType< T > &  NativeType< T > >
	double getLocalMaximum( final RandomAccessibleInterval< T > rai, double[] position, double radius, double calibration )
	{
		// TODO: add out-of-bounds strategy or is this handled by the Neighborhood?
		Shape shape = new HyperSphereShape( (int) Math.ceil( radius / calibration ) );
		final RandomAccessible< Neighborhood< T > > nra = shape.neighborhoodsRandomAccessible( rai );
		final RandomAccess< Neighborhood< T > > neighborhoodRandomAccess = nra.randomAccess();
		neighborhoodRandomAccess.setPosition( getPixelPosition( position, calibration ) );

		final Neighborhood< T > neighborhood = neighborhoodRandomAccess.get();

		final Cursor< T > cursor = neighborhood.cursor();
		double max = - Double.MAX_VALUE;
		double value;
		while( cursor.hasNext() )
		{
			value = cursor.next().getRealDouble();
			if ( value > max )
			{
				max = value;
			}
		}

		return max;
	}

	public static < T extends RealType< T > &  NativeType< T > >
	double getLocalSum( final RandomAccessibleInterval< T > rai, double[] position, double radius, double calibration )
	{
		// TODO: add out-of-bounds strategy or is this handled by the Neighborhood?
		Shape shape = new HyperSphereShape( (int) Math.ceil( radius / calibration ) );
		final RandomAccessible< Neighborhood< T > > nra = shape.neighborhoodsRandomAccessible( rai );
		final RandomAccess< Neighborhood< T > > neighborhoodRandomAccess = nra.randomAccess();
		neighborhoodRandomAccess.setPosition( getPixelPosition( position, calibration ) );

		final Neighborhood< T > neighborhood = neighborhoodRandomAccess.get();

		final Cursor< T > cursor = neighborhood.cursor();
		double sum = 0.0;
		while( cursor.hasNext() )
		{
			sum += cursor.next().getRealDouble();
		}

		return sum;
	}




	private static long[] getPixelPosition( double[] position, double calibration )
	{
		long[] pixelPosition = new long[ position.length ];
		for ( int d = 0; d < position.length; ++d )
		{
			pixelPosition[ d ] = (long) ( position[ d ] / calibration );
		}
		return pixelPosition;
	}

	public static SpimData openSpimData( File file )
	{
		try
		{
			SpimData spimData = new XmlIoSpimData().load( file.toString() );

			return spimData;
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public static ARGBType asArgbType( Color color )
	{
		return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
	}

	public static void loadAndShowSourceFromTiffFile( PlatynereisDataSource dataSource, Bdv bdv )
	{
		ImagePlus imp = IJ.openImage( dataSource.file.toString() );
		Img img = ImageJFunctions.wrap( imp );

		AffineTransform3D prosprScaling = new AffineTransform3D();
		prosprScaling.scale( Constants.PROSPR_SCALING_IN_MICROMETER );

		final BdvStackSource bdvStackSource = BdvFunctions.show( img, dataSource.name, Bdv.options().addTo( bdv ).sourceTransform( prosprScaling ) );
		bdvStackSource.setColor( asArgbType( Constants.DEFAULT_GENE_COLOR ) );
		dataSource.color = Constants.DEFAULT_GENE_COLOR;
		dataSource.bdvStackSource = bdvStackSource;
	}

	public static Bdv showSourceInBdv( PlatynereisDataSource source, Bdv bdv )
	{
		if ( source.isSpimDataMinimal )
		{
			source.bdvStackSource = BdvFunctions.show( source.spimDataMinimal,
					BdvOptions.options()
							.addTo( bdv )
							.transformEventHandlerFactory( new BehaviourTransformEventHandler3DGoogleMouse.BehaviourTransformEventHandler3DFactory() ))
					.get( 0 );

			source.bdvStackSource.setColor( asArgbType( source.color ) );
			source.bdvStackSource.setDisplayRange( 0.0, source.maxLutValue );
		}
		else if ( source.spimData != null)
		{
			source.bdvStackSource = BdvFunctions.show( source.spimData,
					BdvOptions.options()
							.addTo( bdv )
							.transformEventHandlerFactory( new BehaviourTransformEventHandler3DGoogleMouse.BehaviourTransformEventHandler3DFactory() ) )
					.get( 0 );

			source.bdvStackSource.setColor( asArgbType( source.color ) );
			source.bdvStackSource.setDisplayRange( 0.0, source.maxLutValue );
		}
		else if ( source.labelSource != null )
		{
			source.bdvStackSource = BdvFunctions.show( source.labelSource,
					BdvOptions.options()
							.addTo( bdv )
							.transformEventHandlerFactory( new BehaviourTransformEventHandler3DGoogleMouse.BehaviourTransformEventHandler3DFactory() ) );
		}


		BdvOptions.options().addTo( bdv ).transformEventHandlerFactory( new BehaviourTransformEventHandler3DGoogleMouse.BehaviourTransformEventHandler3DFactory() );
		return source.bdvStackSource.getBdvHandle();

	}

	public static void level( Bdv bdv, double[] targetNormalVector )
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		double[] currentNormalVector = getCurrentNormalVector( currentViewerTransform.copy() );
		Utils.log("");
		logVector( "Current normal vector", currentNormalVector );

		LinAlgHelpers.normalize( targetNormalVector ); // just to be sure.

		// determine rotation axis
		double[] rotationAxis = new double[ 3 ];
		LinAlgHelpers.cross( currentNormalVector, targetNormalVector, rotationAxis );
		if ( LinAlgHelpers.length( rotationAxis ) > 0 ) LinAlgHelpers.normalize( rotationAxis );
		logVector( "Rotation axis (data CS)", rotationAxis );

		// The rotation axis is in the coordinate system of the original data set => transform to viewer coordinate system
		double[] qCurrentRotation = new double[ 4 ];
		Affine3DHelpers.extractRotation( currentViewerTransform, qCurrentRotation );
		final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );

		double[] rotationAxisInViewerSystem = new double[ 3 ];
		currentRotation.apply( rotationAxis, rotationAxisInViewerSystem );
		logVector( "Rotation axis (viewer CS)", rotationAxisInViewerSystem );

		// determine rotation angle
		double angle = - Math.acos( LinAlgHelpers.dot( currentNormalVector, targetNormalVector ) );
		Utils.log( "Angle to target normal vector: " + 180.0 / Math.PI * angle );

		// construct rotation of angle around axis
		double[] rotationQuaternion = new double[ 4 ];
		LinAlgHelpers.quaternionFromAngleAxis( rotationAxisInViewerSystem, angle, rotationQuaternion );
		final AffineTransform3D rotation = quaternionToAffineTransform3D( rotationQuaternion );

		// apply transformation (rotating around current viewer centre position)
		final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
		translateCenterToOrigin.translate( DoubleStream.of( Utils.getBdvWindowCenter( bdv )).map( x -> -x ).toArray() );

		final AffineTransform3D translateCenterBack = new AffineTransform3D();
		translateCenterBack.translate( Utils.getBdvWindowCenter( bdv ) );

		ArrayList< AffineTransform3D > viewerTransforms = new ArrayList<>(  );

		viewerTransforms.add( currentViewerTransform.copy()
				.preConcatenate( translateCenterToOrigin )
				.preConcatenate( rotation )
				.preConcatenate( translateCenterBack )	);

		Utils.changeBdvViewerTransform( bdv, viewerTransforms, 2000 );

	}

	private static AffineTransform3D quaternionToAffineTransform3D( double[] rotationQuaternion )
	{
		double[][] rotationMatrix = new double[ 3 ][ 3 ];
		LinAlgHelpers.quaternionToR( rotationQuaternion, rotationMatrix );
		return matrixAsAffineTransform3D( rotationMatrix );
	}

	private static AffineTransform3D matrixAsAffineTransform3D( double[][] rotationMatrix )
	{
		final AffineTransform3D rotation = new AffineTransform3D();
		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++ col)
				rotation.set( rotationMatrix[ row ][ col ], row, col);
		return rotation;
	}

	private static void changeBdvViewerTransform(
			Bdv bdv,
			AffineTransform3D newViewerTransform,
			long duration)
	{

		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(
						currentViewerTransform,
						newViewerTransform,
						0 ,
						0,
						duration );


		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );
		bdv.getBdvHandle().getViewerPanel().transformChanged( newViewerTransform );

	}

	private static void changeBdvViewerTransform(
			Bdv bdv,
			ArrayList< AffineTransform3D > transforms,
			long duration)
	{

		AffineTransform3D currentTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentTransform );

		ArrayList< SimilarityTransformAnimator > animators = new ArrayList<>(  );

		final SimilarityTransformAnimator firstAnimator =
				new SimilarityTransformAnimator(
						currentTransform.copy(),
						transforms.get( 0 ).copy(),
						0 ,
						0,
						duration );

		animators.add( firstAnimator );

		for ( int i = 1; i < transforms.size(); i++ )
		{
			final SimilarityTransformAnimator animator =
					new SimilarityTransformAnimator(
							transforms.get( i - 1 ).copy(),
							transforms.get( i ).copy(),
							0 ,
							0,
							duration );

			animators.add( animator );
		}


		AbstractTransformAnimator transformAnimator = new ConcatenatedTransformAnimator( duration, animators );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( transformAnimator );
		//bdv.getBdvHandle().getViewerPanel().transformChanged( currentTransform.copy() );

	}



	public static void logVector( String preText, double[] vector )
	{
		String text = preText + ": ";

		for ( int i = 0; i < vector.length; ++i )
		{
			text += vector[ i ] + " ";
		}

		Utils.log( text );
	}

	private static double[] getCurrentNormalVector( AffineTransform3D currentViewerTransform )
	{
		final double[] viewerC = new double[]{ 0, 0, 0 };
		final double[] viewerX = new double[]{ 1, 0, 0 };
		final double[] viewerY = new double[]{ 0, 1, 0 };

		final double[] dataC = new double[ 3 ];
		final double[] dataX = new double[ 3 ];
		final double[] dataY = new double[ 3 ];

		final double[] dataV1 = new double[ 3 ];
		final double[] dataV2 = new double[ 3 ];
		final double[] currentNormalVector = new double[ 3 ];

		currentViewerTransform.inverse().apply( viewerC, dataC );
		currentViewerTransform.inverse().apply( viewerX, dataX );
		currentViewerTransform.inverse().apply( viewerY, dataY );

		LinAlgHelpers.subtract( dataX, dataC, dataV1 );
		LinAlgHelpers.subtract( dataY, dataC, dataV2 );

		LinAlgHelpers.cross( dataV1, dataV2, currentNormalVector );

		LinAlgHelpers.normalize( currentNormalVector );

		return currentNormalVector;
	}

	public static double[] getCSVasDoubles( String csv )
	{
		final String[] split = csv.split( "," );
		double[] normalVector = new double[ split.length ];
		for ( int i = 0; i < split.length; ++i )
		{
			normalVector[ i ] = Double.parseDouble( split[ i ] );
		}
		return normalVector;
	}
}
