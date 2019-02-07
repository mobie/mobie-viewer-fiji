package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.tables.TableUtils;
import de.embl.cba.tables.modelview.images.PlatynereisImageSourcesModel;
import de.embl.cba.tables.modelview.images.PlatynereisImageSourcesModelFactory;
import de.embl.cba.tables.modelview.segments.*;
import de.embl.cba.tables.modelview.views.DefaultBdvAndTableView;
import de.embl.cba.tables.modelview.views.bdv.ImageSegmentsBdvView;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Explore>Platynereis Atlas" )
public class ExplorePlatynereisAtlasCommand implements Command
{
	@Parameter ( label = "Platynereis Atlas Folder" )
	public File dataFolder;

	private static final String COLUMN_NAME_LABEL_IMAGE_ID = "label_image_id";

	private LinkedHashMap< String, ArrayList< Object > > columns;

	@Override
	public void run()
	{
		final File segmentsTableFile =
				new File( dataFolder + "/label_attributes/em-segmented-cells-labels-morphology-v2.csv" );

		final ArrayList< ColumnBasedTableRowImageSegment > tableRowImageSegments
				= createAnnotatedImageSegmentsFromTableFile( segmentsTableFile );

		final PlatynereisImageSourcesModel imageSourcesModel
				= new PlatynereisImageSourcesModelFactory( dataFolder ).getModel();

		final DefaultBdvAndTableView view = new DefaultBdvAndTableView( tableRowImageSegments, imageSourcesModel );

		final ImageSegmentsBdvView bdvView = view.getImageSegmentsBdvView();

		new PlatyBrowserMainFrame( bdvView );

	}

	private ArrayList< ColumnBasedTableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			File tableFile )
	{
		columns = TableUtils.columnsFromTableFile( tableFile, null );

		columns = addLabelImageIdColumn();

		final HashMap< ImageSegmentCoordinate, ArrayList< Object > > imageSegmentCoordinateToColumn
				= createImageSegmentCoordinateToColumn();

		final ArrayList< ColumnBasedTableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, imageSegmentCoordinateToColumn );

		return segments;
	}

	private LinkedHashMap< String, ArrayList< Object > > addLabelImageIdColumn()
	{
		final int numRows = columns.values().iterator().next().size();

		final ArrayList< Object > labelImageIdColumn = new ArrayList<>();
		for ( int row = 0; row < numRows; row++ )
		{
			labelImageIdColumn.add( "em-segmented-cells-labels" );
		}

		columns.put( COLUMN_NAME_LABEL_IMAGE_ID, labelImageIdColumn );

		return columns;
	}

	private HashMap< ImageSegmentCoordinate, ArrayList< Object > > createImageSegmentCoordinateToColumn()
	{
		final HashMap< ImageSegmentCoordinate, ArrayList< Object > > imageSegmentCoordinateToColumn
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
