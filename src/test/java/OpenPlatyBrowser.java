import de.embl.cba.platynereis.PlatynereisImageSourcesModel;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserMainFrame;
import de.embl.cba.tables.modelview.images.ImageSourcesModel;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import de.embl.cba.tables.modelview.segments.TableRowImageSegment;
import de.embl.cba.tables.modelview.views.DefaultTableAndBdvViews;
import de.embl.cba.tables.modelview.views.ImageSegmentsBdvView;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.platynereis.platybrowser.ExplorePlatynereisAtlasCommand.createAnnotatedImageSegmentsFromTableFile;

public class OpenPlatyBrowser
{
	public static void main( String[] args )
	{

		File dataFolder = new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );

//		final File segmentsTableFile =
//				new File( dataFolder + "/label_attributes/em-segmented-cells-labels_main_20190212.csv" );
//
//		final LinkedHashMap< String, List< ? > > columns = new LinkedHashMap<>();
//
//		final List< TableRowImageSegment > tableRowImageSegments
//				= createAnnotatedImageSegmentsFromTableFile( segmentsTableFile, columns );
//
//		final PlatynereisImageSourcesModel imageSourcesModel
//				= new PlatynereisImageSourcesModel( dataFolder );
//
//		final DefaultTableAndBdvViews view = new DefaultTableAndBdvViews(
//				tableRowImageSegments,
//				imageSourcesModel );
//
//		view.getTableRowsTableView().categoricalColumnNames().add( "label_id" );
//
//		final ImageSegmentsBdvView bdvView = view.getImageSegmentsBdvView();
//
//		final Map< String, SourceAndMetadata< ? > > sources = imageSourcesModel.sources();

		final PlatyBrowserMainFrame platyBrowserMainFrame =
				new PlatyBrowserMainFrame( dataFolder );

//		platyBrowserMainFrame.getSourcesPanel().addSourceToPanelAndViewer(
//				sources.get( "em-raw-parapod-fib-affine_g" ) );

	}
}
