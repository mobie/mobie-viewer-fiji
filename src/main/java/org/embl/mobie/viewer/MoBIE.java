package org.embl.mobie.viewer;

import bdv.img.n5.N5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.annotate.RegionCreator;
import org.embl.mobie.viewer.annotate.RegionTableRow;
import org.embl.mobie.viewer.plugins.platybrowser.GeneSearchCommand;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.embl.mobie.viewer.source.ImageSource;
import org.embl.mobie.viewer.source.SegmentationSource;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewManager;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.ImgLoader;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MoBIE
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private static MoBIE moBIE;

	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";
	private final String projectName;
	private MoBIESettings settings;
	private String datasetName;
	private Dataset dataset;
	private ViewManager viewManager;
	private Project project;
	private UserInterface userInterface;
	private String projectRoot;
	private String imageRoot;
	private String tableRoot;
	private HashMap< String, ImgLoader > sourceNameToImgLoader;
	private Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter;
	private ArrayList< String > projectCommands = new ArrayList<>();;
	public static int minLogTimeMillis = 100;

	public MoBIE( String projectRoot ) throws IOException
	{
		this( projectRoot, MoBIESettings.settings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		// Only allow one instance to avoid confusion
		if ( moBIE != null )
			moBIE.close();
		moBIE = this;

		// Open project
		IJ.log("\n# MoBIE" );
		IJ.log("Opening project: " + projectLocation );
		WindowArrangementHelper.setLogWindowPositionAndSize();

		this.settings = settings.projectLocation( projectLocation );
		setS3Credentials( settings );
		setProjectImageAndTableRootLocations( );
		registerProjectPlugins( settings.values.getProjectLocation() );
		projectName = MoBIEHelper.getName( projectLocation );
		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
		project = new ProjectJsonParser().parseProject( IOHelper.combinePath( projectRoot,  "project.json" ) );
		this.settings = setImageDataFormat( projectLocation );
		openDataset();
	}

	public Map< String, String > getRegionTableDirectories( RegionDisplay display )
	{
		Map<String, String> sourceNameToTableDir = new HashMap<>();
		final String relativePath = display.getTableDataFolder( TableDataFormat.TabDelimitedFile );
		final String tablesDirectoryPath = getTablesDirectoryPath( relativePath );
		// the source name is the same as the display name
		sourceNameToTableDir.put( display.getName(), tablesDirectoryPath );
		return sourceNameToTableDir;
	}

	// TODO: probably such "plugins" should rather come with the MoBIESettings
	//  such that additional commands could be registered without
	//  changing the core code
	private void registerProjectPlugins( String projectLocation )
	{
		if( projectLocation.contains( "platybrowser" ) )
		{
			GeneSearchCommand.setMoBIE( this );
			projectCommands.add( SourceAndConverterService.getCommandName( GeneSearchCommand.class ) );
		}
	}

	private void setS3Credentials( MoBIESettings settings )
	{
		if ( settings.values.getS3AccessAndSecretKey() != null )
		{
			S3Utils.setS3AccessAndSecretKey( settings.values.getS3AccessAndSecretKey() );
		}
	}

	private MoBIESettings setImageDataFormat( String projectLocation )
	{
		final List<ImageDataFormat> imageDataFormat = settings.values.getImageDataFormat();

		if ( imageDataFormat.size()  != 0 )
		{
			if ( project.getImageDataFormats().stream().noneMatch(imageDataFormat::contains) )
			{
				throw new RuntimeException( "The requested image data format " + imageDataFormat + " is not supported by the project: " + projectLocation );
			}
		}
		else // automatically determine the correct image format
		{
			final List< ImageDataFormat > imageDataFormats = project.getImageDataFormats();
			if ( projectLocation.startsWith( "http" ) )
			{
				if ( imageDataFormats.contains( ImageDataFormat.OmeZarrS3 ) )
					settings.imageDataFormat( ImageDataFormat.OmeZarrS3 );
				if ( imageDataFormats.contains( ImageDataFormat.BdvOmeZarrS3 ) )
					 settings.imageDataFormat( ImageDataFormat.BdvOmeZarrS3 );
				if ( imageDataFormats.contains( ImageDataFormat.BdvN5S3 ) )
					 settings.imageDataFormat( ImageDataFormat.BdvN5S3 );
				if ( imageDataFormats.contains( ImageDataFormat.OpenOrganelleS3 ) )
					 settings.imageDataFormat( ImageDataFormat.OpenOrganelleS3 );
				if (!(imageDataFormats.contains( ImageDataFormat.OmeZarrS3 ) || imageDataFormats.contains( ImageDataFormat.BdvOmeZarrS3 )
                || imageDataFormats.contains( ImageDataFormat.BdvN5S3 ) ||  imageDataFormats.contains( ImageDataFormat.OpenOrganelleS3 )))
					throw new UnsupportedOperationException( "Could not find an S3 storage of the images." );
			}
			else
			{
				if ( imageDataFormats.contains( ImageDataFormat.OmeZarr ) )
					return settings.imageDataFormat( ImageDataFormat.OmeZarr );
				else if ( imageDataFormats.contains( ImageDataFormat.BdvOmeZarr ) )
					return settings.imageDataFormat( ImageDataFormat.BdvOmeZarr );
				else if ( imageDataFormats.contains( ImageDataFormat.BdvN5 ) )
					return settings.imageDataFormat( ImageDataFormat.BdvN5 );
				else
					throw new UnsupportedOperationException( "Could not find a file system storage of the images." );
			}
		}
		return settings;
	}

	public static void mergeRegionTables( List< RegionTableRow > tableRows, Map< String, List< String > > columns )
	{
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		final ArrayList< String > regionIdColumn = TableColumns.getColumn( tableRows, TableColumnNames.REGION_ID );
		referenceColumns.put( TableColumnNames.REGION_ID, regionIdColumn );

		// deal with the fact that the grid ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		MoBIEHelper.toDoubleStrings( regionIdColumn );
		MoBIEHelper.toDoubleStrings( columns.get( TableColumnNames.REGION_ID ) );

		final Map< String, List< String > > columnsForMerging = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, columns );

		for ( Map.Entry< String, List< String > > column : columnsForMerging.entrySet() )
		{
			TableRows.addColumn( tableRows, column.getKey(), column.getValue() );
		}
	}

	private void openDataset() throws IOException
	{
		if ( settings.values.getDataset() != null )
		{
			openDataset( settings.values.getDataset() );
		}
		else
		{
			openDataset( project.getDefaultDataset() );
		}
	}

	private void setProjectImageAndTableRootLocations( )
	{
		projectRoot = createPath(
				settings.values.getProjectLocation(),
				settings.values.getProjectBranch() );

		if( ! IOHelper.exists( IOHelper.combinePath( projectRoot, "project.json" ) ) )
		{
			projectRoot = IOHelper.combinePath( projectRoot, "data" );
		}

		imageRoot = createPath(
				settings.values.getImageDataLocation(),
				settings.values.getImageDataBranch() );

		if( ! IOHelper.exists( IOHelper.combinePath( imageRoot, "project.json" ) ) )
		{
			imageRoot = IOHelper.combinePath( imageRoot, "data" );
		}

		tableRoot = createPath(
				settings.values.getTableDataLocation(),
				settings.values.getTableDataBranch() );

		if( ! IOHelper.exists( IOHelper.combinePath( tableRoot, "project.json" ) ) )
		{
			tableRoot = IOHelper.combinePath( tableRoot, "data" );
		}
	}

	/*
	 * Opens "raw" SourceAndConverters.
	 * Note that they do not yet contain all source transforms that may be applied by a view.
	 * However, sourceAndConverters obtained via the getSourceAndConverter method
	 * are containing all the sourceTransforms.
	 * This can be confusing...
	 */
	public Map< String, SourceAndConverter< ? > > openSourceAndConverters( Collection< String > sources )
	{
		final long startTime = System.currentTimeMillis();

		Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverters = new ConcurrentHashMap< >();

		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		AtomicInteger sourceIndex = new AtomicInteger(0);
		final int numImages = sources.size();
		AtomicInteger sourceLoggingModulo = new AtomicInteger(1);
		AtomicLong lastLogMillis = new AtomicLong( System.currentTimeMillis() );

		for ( String sourceName : sources )
		{
			futures.add(
					MultiThreading.ioExecutorService.submit( () -> {
						String log = getLog( sourceIndex, numImages, sourceLoggingModulo, lastLogMillis );
						sourceNameToSourceAndConverters.put( sourceName, openSourceAndConverter( sourceName, log ) );
					}
				) );
		}
		MultiThreading.waitUntilFinished( futures );

		IJ.log( "Opened " + sourceNameToSourceAndConverters.size() + " image(s) in " + (System.currentTimeMillis() - startTime) + " ms, using " + MultiThreading.getNumIoThreads() + " thread(s).");

		return sourceNameToSourceAndConverters;
	}

	private String getLog( AtomicInteger index, int numTotal, AtomicInteger modulo, AtomicLong lastLogMillis )
	{
		if ( ( index.incrementAndGet() - 1 ) % modulo.get() == 0  )
		{
			final long currentTimeMillis = System.currentTimeMillis();
			if ( currentTimeMillis - lastLogMillis.get() < 4000 )
			{
				modulo.set( modulo.get() * 2 );
			}
			else if ( currentTimeMillis - lastLogMillis.get() > 6000 )
			{
				modulo.set( modulo.get() / 2 );
			}
			lastLogMillis.set( currentTimeMillis );
			return "Opening (" + index.get() + "/" + numTotal + "): ";
		}
		else
		{
			return null;
		}
	}

	private void openDataset( String datasetName ) throws IOException
	{
		IJ.log("Opening dataset: " + datasetName );
		sourceNameToImgLoader = new HashMap<>();
		sourceNameToSourceAndConverter = new ConcurrentHashMap<>();
		setDatasetName( datasetName );
		dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );
		userInterface = new UserInterface( this );
		viewManager = new ViewManager( this, userInterface, dataset.is2D, dataset.timepoints );
		final View view = dataset.views.get( settings.values.getView() );
		view.setName( settings.values.getView() );
		IJ.log( "Opening view: " + view.getName() );
		final long startTime = System.currentTimeMillis();
		viewManager.show( view );
		IJ.log("Opened view: " + view.getName() + ", in " + (System.currentTimeMillis() - startTime) + " ms." );
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

		final String path = IOHelper.combinePath( strings.toArray( new String[0] ) );

		return path;
	}

	public ViewManager getViewManager()
	{
		return viewManager;
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
		try
		{
			IJ.log( "Closing MoBIE..." );
			viewManager.close();
		}
		catch ( Exception e )
		{
			IJ.log( "[ERROR] Could not fully close MoBIE." );
			e.printStackTrace();
		}
		IJ.log( "MoBIE closed." );
	}

	public synchronized ImageSource getSource( String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
	}

	public SourceAndConverter< ? > openSourceAndConverter( String sourceName, String log )
	{
		final ImageSource imageSource = getSource( sourceName );
        Set<ImageDataFormat> tmp = imageSource.imageData.keySet();
        ImageDataFormat imageDataFormat = null;
        for ( ImageDataFormat f : tmp ) {
            if ( settings.values.getImageDataFormat().contains( f ) ) {
                imageDataFormat = f;
            }
        }
        if (imageDataFormat == null) {
            System.err.println( "Error opening: " + imageSource );
            throw new RuntimeException();
        }

		final String imagePath = getImagePath( imageSource, imageDataFormat );
        if( log != null )
            IJ.log( log + imagePath );

		try
		{
			SpimData spimData = tryOpenSpimData( imagePath, imageDataFormat );
			sourceNameToImgLoader.put( sourceName, spimData.getSequenceDescription().getImgLoader() );

			final SourceAndConverterFromSpimDataCreator creator = new SourceAndConverterFromSpimDataCreator( spimData );
			SourceAndConverter< ? > sourceAndConverter = creator.getSetupIdToSourceAndConverter().values().iterator().next();
            // Touch the source once to initiate the cache,
            // as this speeds up future accesses significantly
            sourceAndConverter.getSpimSource().getSource( 0,0 );
			return sourceAndConverter;
		}
		catch ( Exception e )
		{
			System.err.println( "Error opening: " + imagePath );
			e.printStackTrace();
			throw new RuntimeException();
		}
    }

	private SpimData tryOpenSpimData( String imagePath, ImageDataFormat imageDataFormat )
	{
		try
		{
			if ( imageDataFormat.equals( ImageDataFormat.BdvOmeZarrS3) ||
					imageDataFormat.equals( ImageDataFormat.BdvOmeZarr) )
			{
				// TODO enable shared queues
				return ( SpimData ) new SpimDataOpener().openSpimData( imagePath, imageDataFormat );
			}
			else
			{
				return ( SpimData ) new SpimDataOpener().openSpimData( imagePath, imageDataFormat, MultiThreading.sharedQueue );
			}
		}
		catch ( SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}

	public void setDataset( String dataset )
    {
        setDatasetName( dataset );
        viewManager.close();
        userInterface.close();

        try {
            openDataset( datasetName );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public Map<String, View> getViews()
    {
        return dataset.views;
    }

    private String getRelativeTableLocation( SegmentationSource source )
    {
        return source.tableData.get( TableDataFormat.TabDelimitedFile ).relativePath;
    }

    public String getTablesDirectoryPath( SegmentationSource source )
    {
        return getTablesDirectoryPath( getRelativeTableLocation( source ) );
    }

    public String getTablesDirectoryPath( String relativeTableLocation )
    {
        return IOHelper.combinePath( tableRoot, getDatasetName(), relativeTableLocation );
    }

	public String getTablePath( SegmentationSource source, String table )
	{
		return getTablePath( getRelativeTableLocation( source ), table );
	}

	public String getTablePath( String relativeTableLocation, String table )
	{
		return IOHelper.combinePath( tableRoot, getDatasetName(), relativeTableLocation, table );
	}

	public String getDatasetPath( String... files )
	{
		final String datasetRoot = IOHelper.combinePath( projectRoot, getDatasetName() );
		return createPath( datasetRoot, files );
	}

	private String createPath( String root, String[] files )
	{
		String location = root;
		for ( String file : files )
			location = IOHelper.combinePath( location, file );
		return location;
	}

	private List< TableRowImageSegment > loadImageSegmentsTable( String sourceName, String tableName, String log )
	{
		final SegmentationSource tableSource = ( SegmentationSource ) getSource( sourceName );
		final String defaultTablePath = getTablePath( tableSource, tableName );
		if ( log != null )
			IJ.log( log + defaultTablePath );
		final List< TableRowImageSegment > segments = MoBIEHelper.createAnnotatedImageSegmentsFromTableFile( defaultTablePath, sourceName );
		return segments;
	}

	private Map< String, List< String > > loadAdditionalTable( String imageID, String tablePath )
	{
		Logger.log( "Opening additional table: " + tablePath );
		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );
		TableColumns.addLabelImageIdColumn( columns, TableColumnNames.LABEL_IMAGE_ID, imageID );
		return columns;
	}

	private List< Map< String, List< String > > > loadAdditionalTables( List<String> sources, String table )
	{
		final List< Map< String, List< String > > > additionalTables = new CopyOnWriteArrayList<>();

		final long start = System.currentTimeMillis();
		final ExecutorService executorService = MultiThreading.ioExecutorService;
		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( String sourceName : sources )
		{
			futures.add(
				executorService.submit( () -> {
					Map< String, List< String > > columns = loadAdditionalTable( sourceName, getTablePath( ( SegmentationSource ) getSource( sourceName ), table ) );
				additionalTables.add( columns );
				} )
			);
		}
		MultiThreading.waitUntilFinished( futures );

		final long durationMillis = System.currentTimeMillis() - start;

		if ( durationMillis > minLogTimeMillis )
			IJ.log( "Read " + sources.size() + " table(s) in " + durationMillis + " ms, using " + MultiThreading.getNumIoThreads() + " thread(s).");

		return additionalTables;
	}

	public Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter()
	{
		return sourceNameToSourceAndConverter;
	}

	private Collection< List< TableRowImageSegment > > loadPrimarySegmentsTables( SegmentationDisplay segmentationDisplay, String tableName )
	{
		final List< String > segmentationDisplaySources = segmentationDisplay.getSources();
		final ConcurrentHashMap< String, Set< Source< ? > > > sourceNameToRootSources = new ConcurrentHashMap();

		for ( String sourceName : segmentationDisplaySources )
		{
			Set< Source< ? > > rootSources = ConcurrentHashMap.newKeySet();
			MoBIEHelper.fetchRootSources( sourceNameToSourceAndConverter.get( sourceName ).getSpimSource(), rootSources );
			sourceNameToRootSources.put( sourceName, rootSources );
		}

		final Queue< List< TableRowImageSegment > > primaryTables = new ConcurrentLinkedQueue<>();
		final int numTables = getNumTables( segmentationDisplaySources, sourceNameToRootSources );
		final long startTimeMillis = System.currentTimeMillis();
		final AtomicLong lastLogMillis = new AtomicLong(startTimeMillis);
		final AtomicInteger tableLoggingModulo = new AtomicInteger(1);
		final AtomicInteger tableIndex = new AtomicInteger();
		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( String displayedSourceName : segmentationDisplaySources )
		{
			final Set< Source< ? > > rootSources = sourceNameToRootSources.get( displayedSourceName );
			for ( Source rootSource : rootSources )
			{
				futures.add( MultiThreading.ioExecutorService.submit( () ->
				{
					final String log = getLog( tableIndex, numTables, tableLoggingModulo, lastLogMillis );
					final List< TableRowImageSegment > primaryTable = loadImageSegmentsTable( rootSource.getName(), tableName, log );
					primaryTables.add( primaryTable );
				} ) );
			}
		}
		MultiThreading.waitUntilFinished( futures );
		IJ.log( "Read " + numTables + " table(s) in " + (System.currentTimeMillis() - startTimeMillis) + " ms, using " + MultiThreading.getNumIoThreads() + " thread(s).");
		return primaryTables;
	}

	private int getNumTables( List< String > segmentationDisplaySources, ConcurrentHashMap< String, Set< Source< ? > > > sourceNameToRootSources )
	{
		int numTables = 0;
		for ( String displayedSourceName : segmentationDisplaySources )
		{
			final Set< Source< ? > > rootSources = sourceNameToRootSources.get( displayedSourceName );
			numTables += rootSources.size();
		}
		return numTables;
	}

	private Map< String, List< String > > createColumnsForMerging( List< TableRowImageSegment > segments, Map< String, List< String > > newColumns )
	{
		final ArrayList< String > segmentIdColumn = TableColumns.getColumn( segments, TableColumnNames.SEGMENT_LABEL_ID );
		final ArrayList< String > imageIdColumn = TableColumns.getColumn( segments, TableColumnNames.LABEL_IMAGE_ID );
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		referenceColumns.put( TableColumnNames.LABEL_IMAGE_ID, imageIdColumn );
		referenceColumns.put( TableColumnNames.SEGMENT_LABEL_ID, segmentIdColumn );

		// deal with the fact that the label ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		MoBIEHelper.toDoubleStrings( segmentIdColumn );
		MoBIEHelper.toDoubleStrings( newColumns.get( TableColumnNames.SEGMENT_LABEL_ID ) );

		final Map< String, List< String > > columnsForMerging = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, newColumns );

		return columnsForMerging;
	}

	private void mergeSegmentsTable( List< TableRowImageSegment > tableRows, Map< String, List< String > > additionalTable )
	{
		// prepare
		final Map< String, List< String > > columnsForMerging = createColumnsForMerging( tableRows, additionalTable );

		// append
		for ( Map.Entry< String, List< String > > column : columnsForMerging.entrySet() )
		{
			TableRows.addColumn( tableRows, column.getKey(), column.getValue() );
		}
	}

	public void appendSegmentsTables( List< String > imageSourceNames, List< String > relativeTablePaths, List< TableRowImageSegment > tableRows )
	{
		for ( String table : relativeTablePaths )
		{
			// load
			final List< Map< String, List< String > > > additionalTables = loadAdditionalTables( imageSourceNames, table );

			// concatenate
			Map< String, List< String > > concatenatedTable = TableColumns.concatenate( additionalTables );

			// merge
			mergeSegmentsTable( tableRows, concatenatedTable );
		}
	}

	public void appendSegmentsTables( String source, String tablePath, List<TableRowImageSegment> tableRows )
	{
		// load
		Map< String, List< String > > additionalTable = loadAdditionalTable( source, tablePath );

		// merge
		mergeSegmentsTable( tableRows, additionalTable );
	}

	public void appendSegmentsTables( SegmentationDisplay segmentationDisplay, List< String > relativeTablePaths )
	{
		appendSegmentsTables( segmentationDisplay.getSources(), relativeTablePaths, segmentationDisplay.tableRows );
	}

	/**
	 * Primary segment tables must contain the image segment properties.
	 */
	public void loadPrimarySegmentsTables( SegmentationDisplay segmentationDisplay )
	{
		segmentationDisplay.tableRows = new ArrayList<>();
		final Collection< List< TableRowImageSegment > > primaryTables = loadPrimarySegmentsTables( segmentationDisplay, segmentationDisplay.getTables().get( 0 ) );

		for ( List< TableRowImageSegment > primaryTable : primaryTables )
		{
			segmentationDisplay.tableRows.addAll( primaryTable );
		}
	}

	public List< RegionTableRow > createRegionTableRows( RegionDisplay regionDisplay )
	{
		// read
		final List< Map< String, List< String > > > tables = new ArrayList<>();
		for ( String table : regionDisplay.getTables() )
		{
			String tablePath = getTablePath( regionDisplay.getTableDataFolder( TableDataFormat.TabDelimitedFile ), table );
			tablePath = MoBIEHelper.resolveTablePath( tablePath );
			final long startTime = System.currentTimeMillis();
			tables.add( TableColumns.stringColumnsFromTableFile( tablePath ) );
			final long durationMillis = System.currentTimeMillis() - startTime;
			if ( durationMillis > minLogTimeMillis )
				Logger.log( "Read in "+ durationMillis +" ms: " + tablePath );
		}

		// create primary table
		final Map< String, List< String > > referenceTable = tables.get( 0 );
		// TODO: The AnnotatedMaskCreator does not need the sources, but just the source's real intervals
		final RegionCreator regionCreator = new RegionCreator( referenceTable, regionDisplay.getAnnotationIdToSources(), ( String sourceName ) -> sourceNameToSourceAndConverter.get( sourceName )  );
		final List< RegionTableRow > regionTableRows = regionCreator.getRegionTableRows();

		final List< Map< String, List< String > > > additionalTables = tables.subList( 1, tables.size() );

		for ( int i = 0; i < additionalTables.size(); i++ )
		{
			MoBIE.mergeRegionTables( regionTableRows, additionalTables.get( i ) );
		}

		return regionTableRows;
	}

	public void closeSourceAndConverter( SourceAndConverter< ? > sourceAndConverter, boolean closeImgLoader )
	{
		SourceAndConverterServices.getBdvDisplayService().removeFromAllBdvs( sourceAndConverter );
		String sourceName = sourceAndConverter.getSpimSource().getName();

		if ( closeImgLoader )
		{
			final ImgLoader imgLoader = sourceNameToImgLoader.get( sourceName );
			if ( imgLoader instanceof N5ImageLoader )
			{
				( ( N5ImageLoader ) imgLoader ).close();
			}
			else if ( imgLoader instanceof N5OMEZarrImageLoader )
			{
				( ( N5OMEZarrImageLoader ) imgLoader ).close();
			}
		}

		sourceNameToImgLoader.remove( sourceName );
		sourceNameToSourceAndConverter.remove( sourceName );
		SourceAndConverterServices.getSourceAndConverterService().remove( sourceAndConverter );
	}

    public synchronized String getImagePath(ImageSource source, ImageDataFormat imageDataFormat) {

        switch (imageDataFormat) {
            case BdvN5:
            case BdvOmeZarr:
            case OmeZarr:
            case BdvOmeZarrS3:
            case BdvN5S3:
                final String relativePath = source.imageData.get( imageDataFormat ).relativePath;
                return IOHelper.combinePath( imageRoot, getDatasetName(), relativePath );
            case OpenOrganelleS3:
            case OmeZarrS3:
                return source.imageData.get( imageDataFormat ).s3Address;
            default:
                throw new UnsupportedOperationException( "File format not supported: " + imageDataFormat );
        }
    }

	public void addSourceAndConverters( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverters )
	{
		sourceNameToSourceAndConverter.putAll( sourceNameToSourceAndConverters );
	}

	public ArrayList< String > getProjectCommands()
	{
		return projectCommands;
	}

	public Map< String, String > getSegmentationTableDirectories( SegmentationDisplay display )
	{
		Map<String, String> sourceNameToTableDir = new HashMap<>();
		for ( String source: display.getSources() )
		{
			try
			{
				sourceNameToTableDir.put( source, getTablesDirectoryPath( ( SegmentationSource ) getSource( source ) )
				);
			}
			catch ( Exception e )
			{
				System.out.println("[WARNING] Could not store table directory for " + source );
				sourceNameToTableDir.put( source, null );
			}
		}
		return sourceNameToTableDir;
	}
}
