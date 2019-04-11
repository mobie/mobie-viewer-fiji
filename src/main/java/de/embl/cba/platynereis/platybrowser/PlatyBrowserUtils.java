package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.modelview.segments.ImageSegmentCoordinate;
import de.embl.cba.tables.modelview.segments.SegmentUtils;
import de.embl.cba.tables.modelview.segments.TableRowImageSegment;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlatyBrowserUtils
{
	public static final String COLUMN_NAME_LABEL_IMAGE_ID = "label_image_id";

	public static List< TableRowImageSegment > createAnnotatedImageSegmentsFromTableFile( File tableFile, String imageId )
	{
		LinkedHashMap< String, List< ? > > columns = TableColumns.asTypedColumns( TableColumns.stringColumnsFromTableFile( tableFile ) );

		TableColumns.addLabelImageIdColumn(
				columns,
				COLUMN_NAME_LABEL_IMAGE_ID,
				imageId );

		final Map< ImageSegmentCoordinate, List< ? > > imageSegmentCoordinateToColumn
				= createImageSegmentCoordinateToColumn( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns( columns, imageSegmentCoordinateToColumn, false );

		return segments;
	}

	public static Map< ImageSegmentCoordinate, List< ? > > createImageSegmentCoordinateToColumn(
			LinkedHashMap< String, List< ? > > columns )
	{
		final HashMap< ImageSegmentCoordinate, List< ? > > imageSegmentCoordinateToColumn
				= new HashMap<>();

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.LabelImage,
				columns.get( COLUMN_NAME_LABEL_IMAGE_ID ));

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.ObjectLabel,
				columns.get( "label_id" ) );

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.X,
				columns.get( "anchor_x" ) );

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.Y,
				columns.get( "anchor_y" ) );

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.Z,
				columns.get( "anchor_z" ) );

		return imageSegmentCoordinateToColumn;
	}
}
