package de.embl.cba.platynereis.utils;

import bdv.util.*;
import de.embl.cba.bdv.utils.transformhandlers.BehaviourTransformEventHandler3DGoogleMouse;
import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.PlatynereisDataSource;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
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
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

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
			System.out.println( file.toString() );
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
			source.bdvStackSource.setDisplayRange( 0.0, source.maxLutValue );
		}


		BdvOptions.options().addTo( bdv ).transformEventHandlerFactory( new BehaviourTransformEventHandler3DGoogleMouse.BehaviourTransformEventHandler3DFactory() );
		return source.bdvStackSource.getBdvHandle();

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

	public static ImagePlus asImagePlus( RandomAccessibleInterval< BitType > mask, double[] voxelSize )
	{
		RandomAccessibleInterval rai = Views.addDimension( mask, 0, 0 );
		rai = Views.permute( rai, 2,3 );
		final ImagePlus imp = ImageJFunctions.wrap( rai, "" );

		final Calibration calibration = new Calibration();
		calibration.pixelWidth = voxelSize[ 0 ];
		calibration.pixelHeight = voxelSize[ 1 ];
		calibration.pixelDepth = voxelSize[ 2 ];
		imp.setCalibration( calibration );

		return imp;
	}
}
