package de.embl.cba.mobie;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.serialize.DatasetJsonParser;
import de.embl.cba.mobie.serialize.ProjectJsonParser;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.mobie.source.ImageSource;
import de.embl.cba.mobie.source.SegmentationSource;
import de.embl.cba.mobie.table.TableDataFormat;
import de.embl.cba.mobie.ui.UserInterface;
import de.embl.cba.mobie.ui.WindowArrangementHelper;
import de.embl.cba.mobie.view.View;
import de.embl.cba.mobie.view.ViewerManager;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.github.GitHubUtils;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.embl.cba.mobie.Utils.getName;

public class MoBIE
{
	public static final int N_THREADS = 8;
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";

	private final String projectName;
	private MoBIESettings settings;
	private String datasetName;
	private Dataset dataset;
	private ViewerManager viewerManager;
	private Project project;
	private UserInterface userInterface;
	private String projectRoot;
	private String imageRoot;
	private String tableRoot;

	public MoBIE( String projectRoot ) throws IOException
	{
		this( projectRoot, MoBIESettings.settings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		IJ.log("MoBIE");
		this.settings = settings.projectLocation( projectLocation );
		setProjectImageAndTableRootLocations( this.settings );
		projectName = getName( projectLocation );
		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
		project = new ProjectJsonParser().parseProject( FileAndUrlUtils.combinePath( projectRoot,  "project.json" ) );
		openDataset( project.getDefaultDataset() );
	}

	private void setProjectImageAndTableRootLocations( MoBIESettings settings )
	{
		projectRoot = createPath(
				settings.values.getProjectLocation(),
				settings.values.getProjectBranch() );

		if( ! FileAndUrlUtils.exists( FileAndUrlUtils.combinePath( projectRoot, "project.json" ) ) )
		{
			projectRoot = FileAndUrlUtils.combinePath( projectRoot, "data" );
		}

		imageRoot = createPath(
				settings.values.getImageDataLocation(),
				settings.values.getImageDataBranch() );

		if( ! FileAndUrlUtils.exists( FileAndUrlUtils.combinePath( imageRoot, "project.json" ) ) )
		{
			imageRoot = FileAndUrlUtils.combinePath( imageRoot, "data" );
		}

		tableRoot = createPath(
				settings.values.getTableDataLocation(),
				settings.values.getTableDataBranch() );

		if( ! FileAndUrlUtils.exists( FileAndUrlUtils.combinePath( tableRoot, "project.json" ) ) )
		{
			tableRoot = FileAndUrlUtils.combinePath( tableRoot, "data" );
		}
	}

	public List< SourceAndConverter< ? > > openSourceAndConverters( List< String > sources )
	{
		List< SourceAndConverter< ? > > sourceAndConverters = new CopyOnWriteArrayList<>();
		final long start = System.currentTimeMillis();
		final int nThreads = N_THREADS;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );
		for ( String sourceName : sources )
		{
			executorService.execute( () -> {
				sourceAndConverters.add( openSourceAndConverter( sourceName ) );
			} );
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		}

		System.out.println( "Fetched " + sourceAndConverters.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + nThreads + " thread(s).");

		return sourceAndConverters;
	}

	private void openDataset( String datasetName ) throws IOException
	{
		setDatasetName( datasetName );
		dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );

		userInterface = new UserInterface( this );
		viewerManager = new ViewerManager( this, userInterface, dataset.is2D, dataset.timepoints );
		viewerManager.show( dataset.views.get( "default" ) );

		// arrange windows
		WindowArrangementHelper.setLogWindowPositionAndSize( userInterface.getWindow() );
		WindowArrangementHelper.rightAlignWindow( userInterface.getWindow(), viewerManager.getSliceViewer().getWindow(), false, true );
	}

	private void setDatasetName( String datasetName )
	{
		this.datasetName = datasetName;
	}

	private String createPath( String rootLocation, String githubBranch, String... files )
	{
		if ( rootLocation.contains( "github.com" ) )
		{
			rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
		}

		final ArrayList< String > strings = new ArrayList<>();
		strings.add( rootLocation );

		for ( int i = 0; i < files.length; i++ )
		{
			strings.add( files[ i ] );
		}

		final String path = FileAndUrlUtils.combinePath( strings.toArray( new String[0] ) );

		return path;
	}

	public ViewerManager getViewerManager()
	{
		return viewerManager;
	}

	public String getProjectName()
	{
		return projectName;
	}

	public MoBIESettings getSettings()
	{
		return settings;
	}

	public Dataset getDataset()
	{
		return dataset;
	}

	public String getDatasetName()
	{
		return datasetName;
	}

	public List< String > getDatasets()
	{
		return project.getDatasets();
	}

	public UserInterface getUserInterface() { return userInterface; }

	public void close()
	{
		// TODO
//		sourcesDisplayManager.removeAllSourcesFromViewers();
//		sourcesDisplayManager.getBdv().close();
//		userInterface.dispose();
	}

	public synchronized ImageSource getSource( String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
	}

	public SourceAndConverter openSourceAndConverter( String sourceName )
	{
		final ImageSource source = getSource( sourceName );
		final String imagePath = getImagePath( source );
		new Thread( () -> IJ.log( "Opening image:\n" + imagePath ) ).start();
		final SpimData spimData = BdvUtils.openSpimData( imagePath );
		final SourceAndConverterFromSpimDataCreator creator = new SourceAndConverterFromSpimDataCreator( spimData );
		final SourceAndConverter sourceAndConverter = creator.getSetupIdToSourceAndConverter().values().iterator().next();
		return sourceAndConverter;
	}

	public void setDataset( String dataset )
	{
		setDatasetName( dataset );
		viewerManager.close();
		userInterface.close();

		try
		{
			openDataset( datasetName );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public Map< String, View > getViews()
	{
		return dataset.views;
	}

	public synchronized String getImagePath( ImageSource source )
	{
		final ImageDataFormat imageDataFormat = settings.values.getImageDataFormat();

		switch ( imageDataFormat )
		{
			case BdvN5:
			case BdvN5S3:
				final String relativePath = source.imageData.get( imageDataFormat ).relativePath;
				return FileAndUrlUtils.combinePath( imageRoot, getDatasetName(), relativePath );
			case OpenOrganelle:
				final String s3Address = source.imageData.get( imageDataFormat ).s3Address;
				throw new UnsupportedOperationException( "Loading openOrganelle not supported yet.");
			default:
				throw new UnsupportedOperationException( "File format not supported: " + imageDataFormat );

		}
	}

	public String getDefaultTablePath( SegmentationSource source )
	{
		return getTablePath( source.tableData.get( TableDataFormat.TabDelimitedFile ).relativePath, "default.tsv" );
	}

	public String getDefaultTablePath( String relativeTableLocation )
	{
		return getTablePath( relativeTableLocation, "default.tsv" );
	}

	public String getTablePath( String relativeTableLocation, String table )
	{
		return FileAndUrlUtils.combinePath( tableRoot, getDatasetName(), relativeTableLocation, table );
	}

	public String getDatasetPath( String... files )
	{
		final String datasetRoot = FileAndUrlUtils.combinePath( projectRoot, getDatasetName() );
		return createPath( datasetRoot, files );
	}

	private String createPath( String root, String[] files )
	{
		String location = root;
		for ( String file : files )
			location = FileAndUrlUtils.combinePath( location, file );
		return location;
	}
}
