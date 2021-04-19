package de.embl.cba.mobie2;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.image.SourcesModel;
import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.serialize.ProjectJsonParser;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.mobie2.ui.UserInterfaceHelper;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.mobie2.view.ViewerManager;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.github.GitHubUtils;
import mpicbg.spim.data.SpimData;
import net.imglib2.realtransform.AffineTransform3D;
import org.fife.rsta.ac.js.Logger;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static de.embl.cba.mobie.utils.Utils.getName;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setSystemSwingLookAndFeel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setLafSwingLookAndFeel;

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
	private String currentDatasetName;
	private ViewerManager viewerManager;

	public MoBIE2( String projectLocation ) throws IOException
	{
		this( projectLocation, MoBIEOptions.options() );
	}

	public MoBIE2( String projectLocation, MoBIEOptions options ) throws IOException
	{
		this.projectLocation = projectLocation;
		this.options = options;
		projectName = getName( projectLocation );

		setLafSwingLookAndFeel();

		final Project project = new ProjectJsonParser().getProject( getPath( "project.json" ) );
		currentDatasetName = project.datasets.get( 0 );

		dataset = new DatasetJsonParser().getDataset( getPath( getCurrentDatasetName(), "dataset.json" ) );

		final UserInterface userInterface = new UserInterface( this );
		viewerManager = new ViewerManager( this, userInterface, dataset.is2D, dataset.timepoints );
		viewerManager.show( dataset.views.get( "default" ) );

		// arrange windows
		UserInterfaceHelper.setLogWindowPositionAndSize( userInterface.getWindow() );
		UserInterfaceHelper.rightAlignWindow( userInterface.getWindow(), viewerManager.getSliceViewer().getWindow(), false, true );

		setSystemSwingLookAndFeel(); // To prevent other applications being affected

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

	private String getImageDataStorageModality()
	{
		if ( options.values.getImageDataStorageModality().equals( MoBIEOptions.ImageDataStorageModality.S3 ) )
			return "s3store";
		else
			return "fileSystem";
	}

	private String getPath( String... files )
	{
		String location = projectLocation;

		if ( projectLocation.contains( "github.com" ) )
		{
			location = GitHubUtils.createRawUrl( location, options.values.getProjectBranch() );
		}

		final String[] strings = new String[ files.length + 2 ];
		strings[ 0 ] = location;
		strings[ 1 ] = "data";
		for ( int i = 0; i < files.length; i++ )
		{
			strings[ i + 2] = files[ i ];
		}

		location = FileAndUrlUtils.combinePath( strings );

		return location;
	}

	public ViewerManager getViewerManager()
	{
		return viewerManager;
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

	public SourceAndConverter getSourceAndConverter( String sourceName )
	{
		final ImageSource source = getSource( sourceName );
		new Thread( () -> Logger.log( "Opening: " + sourceName ) ).start();
		final SpimData spimData = BdvUtils.openSpimData( getImageLocation( source ) );
		final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );
		// TODO: think about keeping them in a list to avoid reopening
		return sourceAndConverter;
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

	public String getImageLocation( ImageSource source )
	{
		final String location = getPath( getCurrentDatasetName(), source.imageDataLocations.get( getImageDataStorageModality() ) );
		return location;
	}

	public String getDefaultTableLocation( SegmentationSource source )
	{
		final String location = getPath( getCurrentDatasetName(), source.tableDataLocation, "default.tsv" );
		return location;
	}

	public String getDefaultTableLocation( String relativeTableLocation )
	{
		final String location = getPath( getCurrentDatasetName(), relativeTableLocation, "default.tsv" );
		return location;
	}

}
