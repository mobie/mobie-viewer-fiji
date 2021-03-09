package de.embl.cba.mobie.utils;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.Constants;
import de.embl.cba.mobie.image.SourceGroupLabelSourceMetadata;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.*;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowCenter;
import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class Utils
{
	public static double[] delimitedStringToDoubleArray( String s, String delimiter) {

		String[] sA = s.split(delimiter);
		double[] nums = new double[sA.length];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = Double.parseDouble(sA[i].trim());
		}

		return nums;
	}

	@Deprecated // use ColorUtils instead
	public static Color getColor(String name) {
		try {
			return (Color)Color.class.getField(name.toUpperCase()).get(null);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
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

	public static < T extends RealType< T > &  NativeType< T > >
	double getFractionOfNonZeroVoxels( final RandomAccessibleInterval< T > rai, double[] position, double radius, double calibration )
	{
		// TODO: add out-of-bounds strategy or is this handled by the Neighborhood?
		Shape shape = new HyperSphereShape( (int) Math.ceil( radius / calibration ) );
		final RandomAccessible< Neighborhood< T > > nra = shape.neighborhoodsRandomAccessible( rai );
		final RandomAccess< Neighborhood< T > > neighborhoodRandomAccess = nra.randomAccess();
		neighborhoodRandomAccess.setPosition( getPixelPosition( position, calibration ) );

		final Neighborhood< T > neighborhood = neighborhoodRandomAccess.get();

		final Cursor< T > neighborhoodCursor = neighborhood.cursor();

		long numberOfNonZeroVoxels = 0;
		long numberOfVoxels = 0;

		while( neighborhoodCursor.hasNext() )
		{
			numberOfVoxels++;

			final double realDouble = neighborhoodCursor.next().getRealDouble();

			if ( realDouble != 0)
			{
				numberOfNonZeroVoxels++;
			}
		}

		return 1.0 * numberOfNonZeroVoxels / numberOfVoxels;
	}

	public static String[] combine(String[] a, String[] b){
		int length = a.length + b.length;
		String[] result = new String[length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	public static Object[] combine(Object[] a, Object[] b){
		int length = a.length + b.length;
		Object[] result = new Object[length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
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

	public static void logVector( String preText, double[] vector )
	{
		String text = preText + ": ";

		for ( int i = 0; i < vector.length; ++i )
		{
			text += vector[ i ] + " ";
		}

		Utils.log( text );
	}

	public static void logVector( String preText, double[] vector, int numDigits )
	{
		String text = preText + ": ";

		for ( int i = 0; i < vector.length; ++i )
		{
			text += String.format( "%." + numDigits + "f", vector[ i ] ) + " ";
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

	public static void wait100ms()
	{
		try
		{
			Thread.sleep( 100 );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	public static LinkedTreeMap getLinkedTreeMap( InputStream inputStream ) throws IOException
	{
		final JsonReader reader = new JsonReader( new InputStreamReader( inputStream, "UTF-8" ) );
		GsonBuilder builder = new GsonBuilder();
		return builder.create().fromJson( reader, Object.class );
	}

	public static String padLeftSpaces(String inputString, int length) {
		if (inputString.length() >= length) {
			return inputString;
		}
		StringBuilder sb = new StringBuilder();
		while (sb.length() < length - inputString.length()) {
			sb.append(' ');
		}
		sb.append(inputString);

		return sb.toString();
	}

	public static List< TableRowImageSegment > createGroupedSourcesSegmentsFromTableFile(
			String tablePath,
			String imageId,
			SourceGroupLabelSourceMetadata metadata )
	{
		log( "Opening table: " + tablePath );

		tablePath = resolveTablePath( tablePath );

		Map< String, List< String > > columns =
				TableColumns.stringColumnsFromTableFile( tablePath );

		// Add anchor columns, using the metadata
		final ArrayList< List< String > > anchorColumns = new ArrayList<>();
		for ( int d = 0; d < 3; d++ )
		{
			anchorColumns.add( new ArrayList<>() );
		}

		final ArrayList< String > labelIds = new ArrayList<>();
		final ArrayList< String > labelImageIds = new ArrayList<>();
		final List< String > sourceNames = columns.get( Constants.SOURCE_NAME );

		for ( String sourceName : sourceNames )
		{
			final RealInterval interval = metadata.sourceNameToInterval.get( sourceName );
			for ( int d = 0; d < 3; d++ )
			{
				anchorColumns.get( d ).add( String.valueOf( ( interval.realMax( d ) + interval.realMin( d ) ) / 2 ) );
			}

			labelIds.add( String.valueOf( metadata.sourceNameToLabelIndex.get( sourceName ) ) );
			labelImageIds.add( imageId );
		}

		columns.put( Constants.ANCHOR_X, anchorColumns.get( 0 ) );
		columns.put( Constants.ANCHOR_Y, anchorColumns.get( 1 ) );
		columns.put( Constants.ANCHOR_Z, anchorColumns.get( 2 ) );
		columns.put( Constants.SEGMENT_LABEL_ID, labelIds );
		columns.put( Constants.LABEL_IMAGE_ID, labelImageIds );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= createSegmentPropertyToColumn( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
				columns, segmentPropertyToColumn, false );

		return segments;
	}

	public static List< TableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			String tablePath,
			String imageId )
	{
		log( "Opening table: " + tablePath );

		tablePath = resolveTablePath( tablePath );

		Map< String, List< String > > columns =
				TableColumns.stringColumnsFromTableFile( tablePath );

		TableColumns.addLabelImageIdColumn(
				columns,
				Constants.LABEL_IMAGE_ID,
				imageId );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= createSegmentPropertyToColumn( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, segmentPropertyToColumn, false );

		return segments;
	}

	public static String resolveTablePath( String tablePath )
	{
		if ( tablePath.startsWith( "http" ) ) {
			tablePath = FileUtils.resolveTableURL( URI.create(tablePath) );
		} else {
			tablePath = FileUtils.resolveTablePath( tablePath );
		}
		return tablePath;
	}

	public static boolean isRelativePath( String tablePath )
	{
		final BufferedReader reader = Tables.getReader( tablePath );
		final String firstLine;
		try
		{
			firstLine = reader.readLine();
			return firstLine.startsWith( ".." );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return false;
		}
	}

	public static String getRelativePath( String tablePath )
	{
		final BufferedReader reader = Tables.getReader( tablePath );
		try
		{
			String link = reader.readLine();
			return link;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}

	}

	public static Map< SegmentProperty, List< String > > createSegmentPropertyToColumn(
			Map< String, List< String > > columns )
	{
		final HashMap< SegmentProperty, List< String > > segmentPropertyToColumn
				= new HashMap<>();

		segmentPropertyToColumn.put(
				SegmentProperty.LabelImage,
				columns.get( Constants.LABEL_IMAGE_ID ));

		segmentPropertyToColumn.put(
				SegmentProperty.ObjectLabel,
				columns.get( Constants.SEGMENT_LABEL_ID ) );

		segmentPropertyToColumn.put(
				SegmentProperty.X,
				columns.get( Constants.ANCHOR_X ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Y,
				columns.get( Constants.ANCHOR_Y ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Z,
				columns.get( Constants.ANCHOR_Z ) );

		SegmentUtils.putDefaultBoundingBoxMapping( segmentPropertyToColumn, columns );

		return segmentPropertyToColumn;
	}

	/**
	 * Converts a string to a random number between 0 and 1
	 * @param string
	 * @return
	 */
	public static double createRandom( String string )
	{
		double random = string.hashCode() * goldenRatio;
		random = random - ( long ) Math.floor( random );
		return random;
	}

	/**
	 * TODO: Make this more generic, to also work with other things than prospr and mobie
	 *
	 *
	 * @param selectionName
	 * @param isRemoveProspr
	 * @return
	 */
	public static String getSimplifiedSourceName( String selectionName, boolean isRemoveProspr )
	{
		if ( isRemoveProspr )
			selectionName = selectionName.replace( "prospr-", "" );

		selectionName = selectionName.replace( "6dpf-1-whole-", "" );
		selectionName = selectionName.replace( "segmented-", "" );
		selectionName = selectionName.replace( "mask-", "" );
		return selectionName;
	}

	public static ArrayList< String > getSortedList( Collection< String > strings )
	{
		final ArrayList< String > sorted = new ArrayList<>( strings );
		Collections.sort( sorted, new SortIgnoreCase() );
		return sorted;
	}

	public static String createNormalisedViewerTransformString( BdvHandle bdv, double[] position )
	{
		final AffineTransform3D view = createNormalisedViewerTransform( bdv, position );
		final String replace = view.toString().replace( "3d-affine: (", "" ).replace( ")", "" );
		final String collect = Arrays.stream( replace.split( "," ) ).map( x -> "n" + x.trim() ).collect( Collectors.joining( "," ) );
		return collect;
	}

	public static AffineTransform3D createNormalisedViewerTransform( BdvHandle bdv, double[] position )
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdv.getViewerPanel().state().getViewerTransform( view );

		// translate position to upper left corner of the Window (0,0)
		final AffineTransform3D translate = new AffineTransform3D();
		translate.translate( position );
		view.preConcatenate( translate.inverse() );

		// divide by window width
		final int bdvWindowWidth = BdvUtils.getBdvWindowWidth( bdv );
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		view.preConcatenate( scale );

		return view;
	}

	public static double[] getMousePosition( BdvHandle bdv )
	{
		final RealPoint realPoint = new RealPoint( 2 );
		bdv.getViewerPanel().getMouseCoordinates( realPoint );
		final double[] doubles = new double[ 3 ];
		realPoint.localize( doubles );
		return doubles;
	}

	public static AffineTransform3D createUnnormalizedViewerTransform( AffineTransform3D normalisedTransform, BdvHandle bdv )
	{
		final AffineTransform3D transform = normalisedTransform.copy();

		final int bdvWindowWidth = BdvUtils.getBdvWindowWidth( bdv );
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		transform.preConcatenate( scale.inverse() );

		AffineTransform3D translate = new AffineTransform3D();
		translate.translate( getBdvWindowCenter( bdv ) );

		transform.preConcatenate( translate );

		return transform;
	}

	public static AffineTransform3D asAffineTransform3D( double[] doubles )
	{
		final AffineTransform3D view = new AffineTransform3D( );
		view.set( doubles );
		return view;
	}

	public static FinalRealInterval estimateBounds( Source< ? > source )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		source.getSourceTransform( 0, 0, affineTransform3D );
		final FinalRealInterval bounds = affineTransform3D.estimateBounds( source.getSource( 0, 0 ) );
		return bounds;
	}
}
