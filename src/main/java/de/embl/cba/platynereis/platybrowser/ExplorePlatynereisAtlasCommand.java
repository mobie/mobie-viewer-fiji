package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.platynereis.PlatynereisImageSourcesModel;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.modelview.segments.*;
import de.embl.cba.tables.modelview.views.DefaultBdvAndTableView;
import de.embl.cba.tables.modelview.views.bdv.ImageSegmentsBdvView;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.*;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Explore>Platynereis Atlas" )
public class ExplorePlatynereisAtlasCommand implements Command
{
	@Parameter ( label = "Platynereis Atlas Folder", style = "directory")
	public File dataFolder;

	private static final String COLUMN_NAME_LABEL_IMAGE_ID = "label_image_id";

	private LinkedHashMap< String, List< Object > > columns;

	@Override
	public void run()
	{
		final File segmentsTableFile =
				new File( dataFolder + "/label_attributes/em-segmented-cells-labels-morphology-v2.csv" );

		final List< ColumnBasedTableRowImageSegment > tableRowImageSegments
				= createAnnotatedImageSegmentsFromTableFile( segmentsTableFile );

		final PlatynereisImageSourcesModel imageSourcesModel
				= new PlatynereisImageSourcesModel( dataFolder );

		final DefaultBdvAndTableView view = new DefaultBdvAndTableView( tableRowImageSegments, imageSourcesModel );

		final ImageSegmentsBdvView bdvView = view.getImageSegmentsBdvView();

		new PlatyBrowserMainFrame( bdvView );

	}

	private List< ColumnBasedTableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			File tableFile )
	{
		columns = TableColumns.columnsFromTableFile( tableFile, null );

		TableColumns.addLabelImageIdColumn(
				columns,
				COLUMN_NAME_LABEL_IMAGE_ID,
				"em-segmented-cells-labels" );

		final Map< ImageSegmentCoordinate, List< Object > > imageSegmentCoordinateToColumn
				= createImageSegmentCoordinateToColumn();

		final List< ColumnBasedTableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, imageSegmentCoordinateToColumn );

		return segments;
	}

	private Map< ImageSegmentCoordinate, List< Object > > createImageSegmentCoordinateToColumn()
	{
		final HashMap< ImageSegmentCoordinate, List< Object > > imageSegmentCoordinateToColumn
				= new HashMap<>();

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.ImageId,
				columns.get( COLUMN_NAME_LABEL_IMAGE_ID ));

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.LabelId,
				columns.get( "label_id" ) );

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.X,
				columns.get( "com_x_microns" ) );

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.Y,
				columns.get( "com_y_microns" ) );

		imageSegmentCoordinateToColumn.put(
				ImageSegmentCoordinate.Z,
				columns.get( "com_z_microns" ) );

		return imageSegmentCoordinateToColumn;
	}
}
