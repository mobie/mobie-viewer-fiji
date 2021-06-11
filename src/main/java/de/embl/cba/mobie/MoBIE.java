package de.embl.cba.mobie;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.display.SegmentationSourceDisplay;
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
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.embl.cba.mobie.Utils.createAnnotatedImageSegmentsFromTableFile;
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
			case OpenOrganelleS3:
				final String s3Address = source.imageData.get( imageDataFormat ).s3Address;
				throw new UnsupportedOperationException( "Loading openOrganelle not supported yet.");
			default:
				throw new UnsupportedOperationException( "File format not supported: " + imageDataFormat );

		}
	}

	private String getRelativeTableLocation( SegmentationSource source ) {
		return source.tableData.get( TableDataFormat.TabDelimitedFile ).relativePath;
	}

	public String getTablesDirectoryPath( SegmentationSource source ) {
		return getTablesDirectoryPath( getRelativeTableLocation( source ) );
	}

	public String getTablesDirectoryPath( String relativeTableLocation ) {
		return FileAndUrlUtils.combinePath( tableRoot, getDatasetName(), relativeTableLocation );
	}

	public String getTablePath( SegmentationSource source, String table )
	{
		return getTablePath( getRelativeTableLocation( source ), table );
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

	private List< TableRowImageSegment > loadTable( String sourceName, String table )
	{
		final SegmentationSource source = ( SegmentationSource ) getSource( sourceName );

		final String defaultTablePath = getTablePath( source, table );

		final List< TableRowImageSegment > segments = createAnnotatedImageSegmentsFromTableFile( defaultTablePath, sourceName );

		return segments;
	}

	private Map< String, List< String > > loadAdditionalTable( String source, String tablePath )
	{
		Logger.log( "Opening table:\n" + tablePath );
		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );

		TableColumns.addLabelImageIdColumn( columns, Constants.LABEL_IMAGE_ID, source );
		return columns;
	}

	private List< Map< String, List< String > > > loadAdditionalTables( List<String> sources, String table )
	{
		final List< Map< String, List< String > > > additionalTables = new CopyOnWriteArrayList<>();

		final long start = System.currentTimeMillis();
		final ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );

		for ( String sourceName : sources )
		{
			executorService.execute( () -> {
				Map< String, List< String > > columns =
						loadAdditionalTable( sourceName, getTablePath( ( SegmentationSource ) getSource( sourceName ), table ) );
				additionalTables.add( columns );
			} );
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		}

		System.out.println( "Fetched " + sources.size() + " table(s) in " + (System.currentTimeMillis() - start) + " ms, using " + N_THREADS + " thread(s).");

		return additionalTables;
	}


	private ArrayList< List< TableRowImageSegment > > loadPrimaryTables( SegmentationSourceDisplay segmentationDisplay, String table )
	{
		final ArrayList< List< TableRowImageSegment > > primaryTables = new ArrayList<>();

		// TODO: make parallel
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final List< TableRowImageSegment > primaryTable = loadTable( sourceName, table );
			primaryTables.add( primaryTable );
		}

		return primaryTables;
	}

	private Map< String, List< String > > createColumnsForMerging( Map< String, List< String > > newColumns, List< TableRowImageSegment > segments )
	{
		final ArrayList< String > segmentIdColumn = TableColumns.getColumn( segments, Constants.SEGMENT_LABEL_ID );
		final ArrayList< String > imageIdColumn = TableColumns.getColumn( segments, Constants.LABEL_IMAGE_ID );
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		referenceColumns.put( Constants.LABEL_IMAGE_ID, imageIdColumn );
		referenceColumns.put( Constants.SEGMENT_LABEL_ID, segmentIdColumn );

		// deal with the fact that the label ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		Utils.toDoubleStrings( segmentIdColumn );
		Utils.toDoubleStrings( newColumns.get( Constants.SEGMENT_LABEL_ID ) );

		final Map< String, List< String > > columnsForMerging = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, newColumns );

		return columnsForMerging;
	}

	private void mergeTables( List<TableRowImageSegment> tableRows, Map< String, List< String > > tableToMerge )
	{
		// merge
		final Map< String, List< String > > columnsForMerging = createColumnsForMerging( tableToMerge, tableRows );

		// append
		for ( Map.Entry< String, List< String > > column : columnsForMerging.entrySet() )
		{
			TableRows.addColumn( tableRows, column.getKey(), column.getValue() );
		}
	}

	public void appendTables( List<String> sources, List< String > tables, List<TableRowImageSegment> tableRows )
	{
		for ( String table : tables )
		{
			// load
			final List< Map< String, List< String > > > additionalTables = loadAdditionalTables( sources, table );

			// concatenate
			Map< String, List< String > > concatenatedTable = TableColumns.concatenate( additionalTables );

			// merge
			mergeTables( tableRows, concatenatedTable );
		}
	}

	// needed to load a table from an arbitrary location on the filesystem
	public void appendTables( String source, String tablePath, List<TableRowImageSegment> tableRows )
	{
		// load
		Map< String, List< String > > additionalTable = loadAdditionalTable( source, tablePath );

		// merge
		mergeTables( tableRows, additionalTable );
	}

	public void appendTables( SegmentationSourceDisplay segmentationDisplay, List< String > tables )
	{
		appendTables( segmentationDisplay.getSources(), tables, segmentationDisplay.segments );
	}

	/**
	 * Primary tables must contain the image segment properties.
	 */
	public void loadPrimaryTables( SegmentationSourceDisplay segmentationDisplay )
	{
		segmentationDisplay.segments = new ArrayList<>();
		final ArrayList< List< TableRowImageSegment > > primaryTables = loadPrimaryTables( segmentationDisplay, segmentationDisplay.getTables().get( 0 ) );

		for ( List< TableRowImageSegment > primaryTable : primaryTables )
		{
			segmentationDisplay.segments.addAll( primaryTable );
		}
	}
}
