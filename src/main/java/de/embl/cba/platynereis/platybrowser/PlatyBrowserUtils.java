package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlatyBrowserUtils
{
	public static final String COLUMN_NAME_LABEL_IMAGE_ID = "label_image_id";

	public static List< TableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			String tablePath, String imageId )
	{

		String actualPath = getTablePathFromLink( tablePath );


		Map< String, List< String > > columns =
						TableColumns.stringColumnsFromTableFile( actualPath );

		TableColumns.addLabelImageIdColumn(
				columns,
				COLUMN_NAME_LABEL_IMAGE_ID,
				imageId );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= createSegmentPropertyToColumn( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, segmentPropertyToColumn, false );

		return segments;
	}

	public static String getTablePathFromLink( String tablePath )
	{
		String resolvedPath = tablePath;

		while( isLink( resolvedPath ) )
			resolvedPath = tablePath.replace( "", "" ); // TODO

		return resolvedPath;
	}

	public static boolean isLink( String tablePath )
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

	public static Map< SegmentProperty, List< String > > createSegmentPropertyToColumn(
			Map< String, List< String > > columns )
	{
		final HashMap< SegmentProperty, List< String > > segmentPropertyToColumn
				= new HashMap<>();

		segmentPropertyToColumn.put(
				SegmentProperty.LabelImage,
				columns.get( COLUMN_NAME_LABEL_IMAGE_ID ));

		segmentPropertyToColumn.put(
				SegmentProperty.ObjectLabel,
				columns.get( "label_id" ) );

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
