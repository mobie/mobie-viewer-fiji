package de.embl.cba.mobie;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowCenter;

public abstract class Utils
{
	public static < T > SourceAndConverter< T > getSource( List< SourceAndConverter< T > > sourceAndConverters, String name )
	{
		for ( SourceAndConverter< T > sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getSpimSource().getName().equals( name ) )
				return sourceAndConverter;
		}

		return null;
	}

	public static double[] delimitedStringToDoubleArray( String s, String delimiter) {

		String[] sA = s.split(delimiter);
		double[] nums = new double[sA.length];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = Double.parseDouble(sA[i].trim());
		}

		return nums;
	}

	public static void log( String text )
	{
		IJ.log( text );
	}

	public static List< TableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			String tablePath,
			String imageId )
	{
		IJ.log( "Opening table:\n" + tablePath );

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

		segmentPropertyToColumn.put(
				SegmentProperty.T,
				columns.get( Constants.TIMEPOINT ) );

		SegmentUtils.putDefaultBoundingBoxMapping( segmentPropertyToColumn, columns );

		return segmentPropertyToColumn;
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

	public static String getName( String path )
	{
		if ( path.startsWith( "http" ) )
		{
			final String[] split = path.split( "/" );
			return split[ split.length - 1 ];
		}
		else
		{
			return new File( path ).getName();
		}
	}

	public static void toDoubleStrings( List< String > values )
	{
		final int size = values.size();
		for ( int i = 0; i < size; i++ )
		{
			values.set( i, String.valueOf( Double.parseDouble( values.get( i ) ) ) );
		}
	}
}
