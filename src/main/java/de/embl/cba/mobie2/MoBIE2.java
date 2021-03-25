package de.embl.cba.mobie2;

import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie2.json.DatasetJsonParser;
import de.embl.cba.mobie2.json.ProjectJsonParser;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.mobie2.view.Viewer;
import de.embl.cba.tables.FileAndUrlUtils;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static de.embl.cba.mobie.utils.Utils.getName;

public class MoBIE2
{
	private SourcesDisplayManager sourcesDisplayManager;
	private SourcesModel sourcesModel;
	private final MoBIEOptions options;
	private String projectLocation; // without branch, pure github address
	private String datasetLocation; // without branch, pure github address
	private String imagesLocation; // selected dataset
	private String tablesLocation;

	private BookmarkManager bookmarkManager;
	private Datasets datasets;
	private double[] levelingVector;
	private String projectName;
	private AffineTransform3D defaultNormalisedViewerTransform;
	private Dataset dataset;
	private String imageDataLocation;
	private String currentDatasetName;
	private Viewer viewer;

	public MoBIE2( String projectLocation ) throws IOException
	{
		this( projectLocation, MoBIEOptions.options() );
	}

	public MoBIE2( String projectLocation, MoBIEOptions options ) throws IOException
	{
		this.projectLocation = projectLocation;
		this.options = options;
		projectName = getName( projectLocation );

		final Project project = new ProjectJsonParser().getProject( FileAndUrlUtils.combinePath( projectLocation, "project.json" ) );
		currentDatasetName = project.datasets.get( 0 );

		dataset = new DatasetJsonParser().getDataset( FileAndUrlUtils.combinePath( projectLocation, currentDatasetName, "dataset.json" ) );

		final String viewName = dataset.views.keySet().iterator().next();

		imageDataLocation = "local";

		final UserInterface userInterface = new UserInterface( this );

		viewer = new Viewer( this, userInterface, dataset.is2D );
		viewer.show( dataset.views.get( viewName ) );

		// arrange windows
		UserInterfaceHelper.setLogWindowPositionAndSize( userInterface.getFrame() );
		UserInterfaceHelper.setBdvWindowPositionAndSize( viewer.getImageViewer().getBdvHandle(), userInterface.getFrame() );

		//configureDatasetsRootLocations();
		//appendSpecificDatasetLocations(); // TODO: separate this such that this MoBIE class does not need to be re-instantiated


//
//		sourcesModel = new SourcesModel( imagesLocation, options.values.getImageDataStorageModality(), tablesLocation );
//		sourcesDisplayManager = new SourcesDisplayManager( sourcesModel, projectName );
//		bookmarkManager = fetchBookmarks( this.projectLocation );
//		levelingVector = fetchLeveling( imagesLocation );
//
//		SwingUtilities.invokeLater( () -> {
//			userInterface = new UserInterface( this );
//			bookmarkManager.setView( "default" );
//			final BdvHandle bdvHandle = sourcesDisplayManager.getBdv();
//			userInterface.setBdvWindowPositionAndSize( bdvHandle );
//			defaultNormalisedViewerTransform = Utils.createNormalisedViewerTransform( bdvHandle, BdvUtils.getBdvWindowCenter( bdvHandle ) );
//			new BdvBehaviourInstaller( this ).run();
//		} );
	}

	public Viewer getViewer()
	{
		return viewer;
	}

	public String getProjectName()
	{
		return projectName;
	}

	public MoBIEOptions getOptions()
	{
		return options;
	}

	public AffineTransform3D getDefaultNormalisedViewerTransform()
	{
		return defaultNormalisedViewerTransform;
	}

	public double[] getLevelingVector()
	{
		return levelingVector;
	}

	public String getProjectLocation()
	{
		return projectLocation;
	}

	public String getCurrentDatasetName()
	{
		return currentDatasetName;
	}

	public ArrayList< String > getDatasets()
	{
		return datasets.datasets;
	}

	public BookmarkManager getBookmarkManager()
	{
		return bookmarkManager;
	}

	public void close()
	{
		// TODO
//		sourcesDisplayManager.removeAllSourcesFromViewers();
//		sourcesDisplayManager.getBdv().close();
//		userInterface.dispose();
	}

	public ImageSource getSource( String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
	}

	public Dataset getDataset()
	{
		return dataset;
	}

	public HashMap< String, View > getViews()
	{
		// combine the individual source views...
		final HashMap< String, View > views = new HashMap<>();
		for ( String sourceName : dataset.sources.keySet() )
		{
			views.put( sourceName, dataset.sources.get( sourceName ).get().view );
		}

		// ...with the additional views
		views.putAll( dataset.views );

		return views;
	}

	public String getAbsoluteImageLocation( ImageSource source )
	{
		return FileAndUrlUtils.combinePath( getProjectLocation(), getCurrentDatasetName(), source.imageDataLocations.get( imageDataLocation ) );
	}

	public String getAbsoluteDefaultTableLocation( SegmentationSource source )
	{
		return FileAndUrlUtils.combinePath( getProjectLocation(), getCurrentDatasetName(), source.tableDataLocation, "default.tsv"  );
	}
}
