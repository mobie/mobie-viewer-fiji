package de.embl.cba.platynereis;

import bdv.BigDataViewer;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.*;
import bdv.viewer.animate.SimilarityTransformAnimator;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imagej.ops.Ops;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.awt.*;
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
		final AffineTransform3D newViewerTransform = getNewViewerTransform( position, scale, bdv, null );

		final double cX = 0; //- bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
		final double cY = 0; //- bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( currentViewerTransform );

		final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator( currentViewerTransform, newViewerTransform, cX ,cY, 3000 );

		bdv.getBdvHandle().getViewerPanel().setTransformAnimator( similarityTransformAnimator );

		bdv.getBdvHandle().getViewerPanel().transformChanged( currentViewerTransform );
	}

	private static AffineTransform3D getNewViewerTransform( double[] position, double scale, Bdv bdv, AffineTransform3D currentViewerTransform )
	{
		final AffineTransform3D newViewerTransform = new AffineTransform3D();

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

		newViewerTransform.setTranslation( translation );
		newViewerTransform.scale( scale );

		double[] translation2 = new double[ 3 ];

		for( int d = 0; d < 3; ++d )
		{
//			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			translation2[ d ] = + bdvWindowDimensions[ d ] / 2.0;
		}

		newViewerTransform.translate( translation2 );

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

		final BdvSource bdvSource = BdvFunctions.show( img, dataSource.name, Bdv.options().addTo( bdv ).sourceTransform( prosprScaling ) );
		bdvSource.setColor( asArgbType( Constants.DEFAULT_GENE_COLOR ) );
		dataSource.color = Constants.DEFAULT_GENE_COLOR;
		dataSource.bdvSource = bdvSource;
	}

	public static Bdv showSourceInBdv( PlatynereisDataSource source, Bdv bdv )
	{
		if ( source.isSpimDataMinimal )
		{
			// setName( source.name, source );
			source.bdvSource = BdvFunctions.show( source.spimDataMinimal,
					BdvOptions.options().addTo( bdv ) ).get( 0 );
			source.bdvSource.setColor( asArgbType( source.color ) );
			source.bdvSource.setDisplayRange( 0.0, source.maxLutValue );

		}
		else
		{
			// setName( dataSourceName, source );
			source.bdvSource = BdvFunctions.show( source.spimData, BdvOptions.options().addTo( bdv ) ).get( 0 );
			source.bdvSource.setColor( asArgbType( source.color ) );
			source.bdvSource.setDisplayRange( 0.0, source.maxLutValue );
		}

		return source.bdvSource.getBdvHandle();

	}
}
