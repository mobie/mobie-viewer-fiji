package de.embl.cba.mobie2;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.serialize.ProjectJsonParser;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.ui.UserInterface;
import de.embl.cba.mobie2.ui.WindowArrangementHelper;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.mobie2.view.ViewerManager;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.github.GitHubUtils;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.embl.cba.mobie.utils.Utils.getName;

public class MoBIE2
{
	public static final int N_THREADS = 8;

	private final String projectName;
	private MoBIEOptions options;
	private String projectLocation; // without branch, pure github address
	private String datasetName;
	private Dataset dataset;
	private ViewerManager viewerManager;
	private Project project;
	private UserInterface userInterface;

	public MoBIE2( String projectLocation ) throws IOException
	{
		this( projectLocation, MoBIEOptions.options() );
	}

	public MoBIE2( String projectLocation, MoBIEOptions options ) throws IOException
	{
		this.projectLocation = projectLocation;
		this.options = options.projectLocation( projectLocation );
		projectName = getName( projectLocation );
		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );

		IJ.log("MoBIE");

		project = new ProjectJsonParser().getProject( getPath( options.values.getProjectLocation(), options.values.getProjectBranch(), "project.json" ) );

		openDataset( project.getDefaultDataset() );
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
		this.datasetName = datasetName;
		dataset = new DatasetJsonParser().parseDataset( getPath( options.values.getProjectLocation(), options.values.getProjectBranch(), getDatasetName(), "dataset.json" ) );

		userInterface = new UserInterface( this );
		viewerManager = new ViewerManager( this, userInterface, dataset.is2D, dataset.timepoints );
		viewerManager.show( dataset.views.get( "default" ) );

		// arrange windows
		WindowArrangementHelper.setLogWindowPositionAndSize( userInterface.getWindow() );
		WindowArrangementHelper.rightAlignWindow( userInterface.getWindow(), viewerManager.getSliceViewer().getWindow(), false, true );
	}

	private String getImageDataStorageModality()
	{
		if ( options.values.getImageDataStorageModality().equals( MoBIEOptions.ImageDataStorageModality.S3 ) )
			return "s3store";
		else
			return "fileSystem";
	}

	private String getPath( String rootLocation, String githubBranch, String... files )
	{
		if ( rootLocation.contains( "github.com" ) )
		{
			rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
		}

		final String[] strings = new String[ files.length + 2 ];
		strings[ 0 ] = rootLocation;
		strings[ 1 ] = "data";
		for ( int i = 0; i < files.length; i++ )
		{
			strings[ i + 2] = files[ i ];
		}

		String path = FileAndUrlUtils.combinePath( strings );

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

	public MoBIEOptions getOptions()
	{
		return options;
	}

	public String getProjectLocation()
	{
		return projectLocation;
	}

	public String getDatasetName()
	{
		return datasetName;
	}

	public List< String > getDatasets()
	{
		return project.getDatasets();
	}

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

	public String getDataset()
	{
		return datasetName;
	}

	public void setDataset( String dataset )
	{
		this.datasetName = dataset;
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
//		final HashMap< String, View > views = new LinkedHashMap<>();
//
//		// combine the individual source views...
//		for ( String sourceName : dataset.sources.keySet() )
//		{
//			views.put( sourceName, dataset.sources.get( sourceName ).get().view );
//		}
//
//		// ...with the additional views
//		views.putAll( dataset.views );

		return dataset.views;
	}

	public synchronized String getImagePath( ImageSource source )
	{
		final String path = getPath( options.values.getImageDataLocation(), options.values.getProjectBranch(), getDatasetName(), source.imageDataLocations.get( getImageDataStorageModality() ) );

		return path;
	}

	public String getDefaultTablePath( SegmentationSource source )
	{
		return getTablePath( source.tableDataLocation, "default.tsv" );
	}

	public String getDefaultTablePath( String relativeTableLocation )
	{
		return getTablePath( relativeTableLocation, "default.tsv" );
	}

	public String getTablePath( String relativeTableLocation, String table )
	{
		final String path = getPath( options.values.getTableDataLocation(), options.values.getTableDataBranch(), getDatasetName(), relativeTableLocation, table ); //+".tsv"
		return path;
	}

}
