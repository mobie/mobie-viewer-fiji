package de.embl.cba.platynereis;

import bdv.BigDataViewer;
import bdv.util.Bdv;
import bdv.util.BdvHandle;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imagej.ops.Ops;
import net.imglib2.*;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		final AffineTransform3D viewerTransform = new AffineTransform3D();

//		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( viewerTransform );


		int[] bdvWindowDimensions = new int[ 3 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();


		double[] translation = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
//			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			translation[ d ] = - position[ d ];
		}

		viewerTransform.setTranslation( translation );
		viewerTransform.scale( scale );

		double[] translation2 = new double[ 3 ];

		for( int d = 0; d < 3; ++d )
		{
//			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			translation2[ d ] = + bdvWindowDimensions[ d ] / 2.0;
		}

		viewerTransform.translate( translation2 );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( viewerTransform );

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
	double getLocalMaximum( RandomAccessibleInterval< T > rai, double[] position, double radius, double calibration )
	{
		Shape shape = new HyperSphereShape( (int) Math.ceil( radius / calibration ) );
		final RandomAccessible< Neighborhood< T > > nra = shape.neighborhoodsRandomAccessible( rai );
		final RandomAccess< Neighborhood< T > > neighborhoodRandomAccess = nra.randomAccess();

		for ( int d = 0; d < position.length; ++d )
		{
			position[ d ] /= calibration;
		}

		neighborhoodRandomAccess.setPosition( Utils.asLongs( position )  );

		final Neighborhood< T > neighborhood = neighborhoodRandomAccess.get();
		double max = - Double.MAX_VALUE;

		final Cursor< T > cursor = neighborhood.cursor();
		while( cursor.hasNext() )
		{
			if ( cursor.next().getRealDouble() > max )
			{
				max = cursor.get().getRealDouble();
			}
		}

		return max;
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
}
