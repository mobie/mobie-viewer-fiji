package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.platynereis.Globals;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatyBrowserUtils
{
	public static
	List< TableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			String tablePath,
			String imageId )
	{
		Utils.log( "Opening table: " + tablePath );

		if ( tablePath.startsWith( "http" ) )
			tablePath = resolveTableURL( URI.create( tablePath ) );

		Map< String, List< String > > columns =
						TableColumns.stringColumnsFromTableFile( tablePath );

		TableColumns.addLabelImageIdColumn(
				columns,
				Globals.COLUMN_NAME_LABEL_IMAGE_ID,
				imageId );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= createSegmentPropertyToColumn( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, segmentPropertyToColumn, false );

		return segments;
	}

	public static String resolveTableURL( URI uri )
	{
		while( isRelativePath( uri.toString() ) )
		{
			URI relativeURI = URI.create( getRelativePath( uri.toString() ) );
			uri = uri.resolve( relativeURI ).normalize();
		}

		return uri.toString();
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
				columns.get( Globals.COLUMN_NAME_LABEL_IMAGE_ID ));

		segmentPropertyToColumn.put(
				SegmentProperty.ObjectLabel,
				columns.get( Globals.COLUMN_NAME_SEGMENT_LABEL_ID ) );

		segmentPropertyToColumn.put(
				SegmentProperty.X,
				columns.get( "anchor_x" ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Y,
				columns.get( "anchor_y" ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Z,
				columns.get( "anchor_z" ) );

		SegmentUtils.putDefaultBoundingBoxMapping( segmentPropertyToColumn, columns );

		return segmentPropertyToColumn;
	}
}
