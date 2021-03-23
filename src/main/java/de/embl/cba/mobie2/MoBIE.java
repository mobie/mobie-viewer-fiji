package de.embl.cba.mobie2;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie.ui.UserInterface;
import de.embl.cba.mobie2.json.DatasetJsonParser;
import de.embl.cba.mobie2.json.ProjectJsonParser;
import de.embl.cba.tables.FileAndUrlUtils;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static de.embl.cba.mobie.utils.Utils.getName;

public class MoBIE
{
	private SourcesDisplayManager sourcesDisplayManager;
	private SourcesModel sourcesModel;
	private final MoBIEOptions options;
	private UserInterface userInterface;
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

	public MoBIE( String projectLocation ) throws IOException
	{
		this( projectLocation, MoBIEOptions.options() );
	}

	public MoBIE( String projectLocation, MoBIEOptions options ) throws IOException
	{
		this.projectLocation = projectLocation;
		this.options = options;
		projectName = getName( projectLocation );

		final Project project = new ProjectJsonParser().getProject( FileAndUrlUtils.combinePath( projectLocation, "project.json" ) );
		currentDatasetName = project.datasets.get( 0 );

		dataset = new DatasetJsonParser().getDataset( FileAndUrlUtils.combinePath( projectLocation, currentDatasetName, "dataset.json" ) );

		final String viewName = dataset.views.keySet().iterator().next();

		imageDataLocation = "local";

		final Viewer viewer = new Viewer( this, dataset.is2D );
		viewer.show( dataset.views.get( viewName ) );


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

	public String getTablesLocation()
	{
		return tablesLocation;
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

	public SourcesModel getSourcesModel()
	{
		return sourcesModel;
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

	private double[] fetchLeveling( String dataLocation )
	{
		final String levelingFile = FileAndUrlUtils.combinePath( dataLocation, "misc/leveling.json" );
		try
		{
			InputStream is = FileAndUrlUtils.getInputStream( levelingFile );
			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
			final GsonBuilder gsonBuilder = new GsonBuilder();
			LinkedTreeMap linkedTreeMap = gsonBuilder.create().fromJson( reader, Object.class );
			ArrayList< Double >  normalVector = ( ArrayList< Double > ) linkedTreeMap.get( "NormalVector" );
			final double[] doubles = normalVector.stream().mapToDouble( Double::doubleValue ).toArray();
			return doubles;
		}
		catch ( Exception e )
		{
			return null; // new double[]{0.70,0.56,0.43};
		}
	}


	private void configureDatasetsRootLocations( )
	{
		this.datasetLocation = datasetLocation;
		this.imagesLocation = options.values.getImageDataLocation() != null ? options.values.getImageDataLocation() : datasetLocation;
		this.tablesLocation = options.values.getTableDataLocation() != null ? options.values.getTableDataLocation() : datasetLocation;

		datasetLocation = FileAndUrlUtils.removeTrailingSlash( datasetLocation );
		imagesLocation = FileAndUrlUtils.removeTrailingSlash( imagesLocation );
		tablesLocation = FileAndUrlUtils.removeTrailingSlash( tablesLocation );

		datasetLocation = adaptUrl( datasetLocation, options.values.getProjectBranch() );
		imagesLocation = adaptUrl( imagesLocation, options.values.getProjectBranch() );
		tablesLocation = adaptUrl( tablesLocation, options.values.getProjectBranch() );

		// two different locations of the data w.r.t someLocation are supported:
		// the data can either be directly underneath someLocation
		// or in someLocation/data
		// test if someLocation/data exists and set if to the new location if it does
		// NOTE this produces a corner case for nested "data" folders, i.e. "data/data", but it's the best solution I came up with so far
		if(FileAndUrlUtils.exists( datasetLocation + "/data/datasets.json")) {
			datasetLocation = datasetLocation + "/data";
		}
		if(FileAndUrlUtils.exists(imagesLocation + "/data/datasets.json")) {
			imagesLocation = imagesLocation + "/data";
		}
		if(FileAndUrlUtils.exists(tablesLocation + "/data/datasets.json")) {
			tablesLocation = tablesLocation + "/data";
		}
	}

	public static String adaptUrl( String url, String projectBranch )
	{
		if ( url.contains( "github.com" ) )
		{
			url = url.replace( "github.com", "raw.githubusercontent.com" );
			url += "/" + projectBranch;
		}
		return url;
	}

//	private BookmarkManager fetchBookmarks( String location )
//	{
//		BookmarkReader bookmarkParser = new BookmarkReader(location);
//		Map< String, Bookmark > nameToBookmark = bookmarkParser.readDefaultBookmarks();
//
//		return new BookmarkManager( this, nameToBookmark, bookmarkParser);
//	}

	// TODO: This should be dataset dependent?
	public SourcesDisplayManager getSourcesDisplayManager()
	{
		return sourcesDisplayManager;
	}

	public void close()
	{
		sourcesDisplayManager.removeAllSourcesFromViewers();
		sourcesDisplayManager.getBdv().close();
		userInterface.dispose();
	}

	public MoBIESource getSource( String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
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
