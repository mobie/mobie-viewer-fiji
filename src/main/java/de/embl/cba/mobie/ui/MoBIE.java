package de.embl.cba.mobie.ui;

import bdv.util.BdvHandle;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie2.ui.WindowArrangementHelper;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.mobie.utils.Utils;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.mobie.utils.Utils.getName;

public class MoBIE
{
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";

	private final SourcesDisplayManager sourcesDisplayManager;
	private final SourcesModel sourcesModel;
	private final MoBIESettings options;
	private UserInterface userInterface;
	private String dataset;
	private final String projectBaseLocation; // without branch, pure github address
	private String projectLocation; // with branch, actual url to data
	private String imagesLocation; // selected dataset
	private String tablesLocation;

	private BookmarkManager bookmarkManager;
	private Datasets datasets;
	private final double[] levelingVector;
	private String projectName;
	private AffineTransform3D defaultNormalisedViewerTransform;

	public MoBIE( String projectLocation )
	{
		this( projectLocation, MoBIESettings.settings() );
	}

	@Deprecated
	public MoBIE(
			String projectLocation,
			String tablesLocation )
	{
		this( projectLocation, MoBIESettings.settings().tableDataLocation( tablesLocation ) );
	}

	public MoBIE(
			String projectBaseLocation,
			MoBIESettings options )
	{
		this.projectBaseLocation = projectBaseLocation;
		this.options = options;
		projectName = getName( this.projectBaseLocation );

		configureDatasetsRootLocations();
		appendSpecificDatasetLocations(); // TODO: separate this such that this MoBIE class does not need to be re-instantiated

		sourcesModel = new SourcesModel( imagesLocation, options.values.getImageDataStorageModality(), tablesLocation );
		sourcesDisplayManager = new SourcesDisplayManager( sourcesModel, projectName );
		bookmarkManager = fetchBookmarks( projectLocation );
		levelingVector = fetchLeveling( imagesLocation );

		SwingUtilities.invokeLater( () -> {
			userInterface = new UserInterface( this );
			WindowArrangementHelper.setImageJLogWindowPositionAndSize( userInterface.getWindow() );
			bookmarkManager.setView( "default" );
			final BdvHandle bdvHandle = sourcesDisplayManager.getBdv();
			WindowArrangementHelper.setBdvWindowPositionAndSize( bdvHandle, userInterface.getWindow() );
			defaultNormalisedViewerTransform = Utils.createNormalisedViewerTransform( bdvHandle, BdvUtils.getBdvWindowCenter( bdvHandle ) );
			new BdvBehaviourInstaller( this ).run();
		} );
	}

	public String getTablesLocation()
	{
		return tablesLocation;
	}

	public String getProjectName()
	{
		return projectName;
	}

	public MoBIESettings getOptions()
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
		return projectBaseLocation; // without branch information, which is stored in the options
	}

	public String getDataset()
	{
		return dataset;
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

	private void appendSpecificDatasetLocations()
	{
		this.datasets = new DatasetsParser().fetchProjectDatasets( projectLocation );
		this.dataset = options.values.getDataset();

		if ( dataset == null )
		{
			dataset = datasets.defaultDataset;
		}

		projectLocation = FileAndUrlUtils.combinePath( projectLocation, dataset );
		imagesLocation = FileAndUrlUtils.combinePath( imagesLocation, dataset );
		tablesLocation = FileAndUrlUtils.combinePath( tablesLocation, dataset );

		Utils.log( "Fetching project from: " + projectLocation );
		Utils.log( "Fetching images from: " + imagesLocation );
		Utils.log( "Image data storage modality: " + options.values.getImageDataStorageModality() );
		Utils.log( "Fetching tables from: " + tablesLocation );
	}

	private void configureDatasetsRootLocations( )
	{
		this.projectLocation = projectBaseLocation;
		this.imagesLocation = options.values.getImageDataLocation() != null ? options.values.getImageDataLocation() : projectBaseLocation;
		this.tablesLocation = options.values.getTableDataLocation() != null ? options.values.getTableDataLocation() : projectBaseLocation;

		projectLocation = FileAndUrlUtils.removeTrailingSlash( projectLocation );
		imagesLocation = FileAndUrlUtils.removeTrailingSlash( imagesLocation );
		tablesLocation = FileAndUrlUtils.removeTrailingSlash( tablesLocation );

		projectLocation = adaptUrl( projectLocation, options.values.getProjectBranch() );
		imagesLocation = adaptUrl( imagesLocation, options.values.getProjectBranch() );
		tablesLocation = adaptUrl( tablesLocation, options.values.getProjectBranch() );

		// two different locations of the data w.r.t someLocation are supported:
		// the data can either be directly underneath someLocation
		// or in someLocation/data
		// test if someLocation/data exists and set if to the new location if it does
		// NOTE this produces a corner case for nested "data" folders, i.e. "data/data", but it's the best solution I came up with so far
		if(FileAndUrlUtils.exists(projectLocation + "/data/datasets.json")) {
			projectLocation = projectLocation + "/data";
		}
		if(FileAndUrlUtils.exists(imagesLocation + "/data/datasets.json")) {
			imagesLocation = imagesLocation + "/data";
		}
		if(FileAndUrlUtils.exists(tablesLocation + "/data/datasets.json")) {
			tablesLocation = tablesLocation + "/data";
		}
	}

	private String adaptUrl( String url, String projectBranch )
	{
		if ( url.contains( "github.com" ) )
		{
			url = url.replace( "github.com", "raw.githubusercontent.com" );
			url += "/" + projectBranch;
		}
		return url;
	}

	private BookmarkManager fetchBookmarks( String location )
	{
		BookmarkReader bookmarkParser = new BookmarkReader(location);
		Map< String, Bookmark > nameToBookmark = bookmarkParser.readDefaultBookmarks();

		return new BookmarkManager( this, nameToBookmark, bookmarkParser);
	}

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
}
