/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.viewer;

import bdv.img.n5.N5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.tables.tablerow.TableRow;
import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.viewer.display.AnnotationDisplay;
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
import org.embl.mobie.viewer.table.TableHelper;
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

import static org.embl.mobie.viewer.MoBIEHelper.selectCommonTableFileNameFromProject;

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
	public static boolean initiallyShowSourceNames = false;

	public MoBIE( String projectRoot ) throws IOException
	{
		this( projectRoot, MoBIESettings.settings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		// Only allow one instance to avoid confusion
		if ( moBIE != null )
		{
			IJ.log("Detected running MoBIE instance.");
			moBIE.close();
		}
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
		setImageDataFormats( projectLocation );
		openDataset();
	}

	public String addColumnsFromProject( AnnotationDisplay< ? extends TableRow > display )
	{
		final Map< String, String > sourceNameToTableDirectory = getTableDirectories( display );
		String tableFileName = selectCommonTableFileNameFromProject( sourceNameToTableDirectory.values(), "Table" );
		if ( tableFileName == null )
			return null;

		if ( display instanceof RegionDisplay )
		{
			for ( String tableDir : sourceNameToTableDirectory.values() )
			{
				String resolvedPath = MoBIEHelper.resolveTablePath( IOHelper.combinePath( tableDir, tableFileName ) );
				IJ.log( "Opening table:\n" + resolvedPath );
				final Map< String, List< String > > tableColumns = TableColumns.stringColumnsFromTableFile( resolvedPath );
				TableHelper.appendRegionTableColumns( display.tableRows, tableColumns );
			}
		}
		else if ( display instanceof SegmentationDisplay )
		{
			ArrayList< String > tableNames = new ArrayList<>( );
			tableNames.add( tableFileName );
			moBIE.appendSegmentTableColumns( display.tableRows, sourceNameToTableDirectory.keySet(), tableNames );
		}

		return tableFileName;
	}

	private Map< String, String > getTableDirectories( AnnotationDisplay display )
	{
		if ( display instanceof RegionDisplay )
			return getRegionTableDirectories( ( RegionDisplay ) display );
		else if ( display instanceof SegmentationDisplay )
			return getSegmentationTableDirectories( ( SegmentationDisplay ) display );
		else
			throw new RuntimeException();
	}

	private Map< String, String > getRegionTableDirectories( RegionDisplay display )
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

	private void setImageDataFormats( String projectLocation )
	{
		final Set< ImageDataFormat > imageDataFormat = settings.values.getImageDataFormats();

		if ( imageDataFormat.size() > 0 )
		{
			if ( project.getImageDataFormats().stream().noneMatch(imageDataFormat::contains) )
			{
				throw new RuntimeException( "The requested image data format " + imageDataFormat + " is not supported by the project: " + projectLocation );
			}
		}
		else
		{
			final List< ImageDataFormat > imageDataFormats = project.getImageDataFormats();
			if ( projectLocation.startsWith( "http" ) )
			{
				if ( imageDataFormats.contains( ImageDataFormat.OmeZarrS3 ) )
					 settings.addImageDataFormat( ImageDataFormat.OmeZarrS3 );
				if ( imageDataFormats.contains( ImageDataFormat.BdvOmeZarrS3 ) )
					 settings.addImageDataFormat( ImageDataFormat.BdvOmeZarrS3 );
				if ( imageDataFormats.contains( ImageDataFormat.BdvN5S3 ) )
					 settings.addImageDataFormat( ImageDataFormat.BdvN5S3 );
				if ( imageDataFormats.contains( ImageDataFormat.OpenOrganelleS3 ) )
					 settings.addImageDataFormat( ImageDataFormat.OpenOrganelleS3 );
				if ( ! ( imageDataFormats.contains( ImageDataFormat.OmeZarrS3 ) || imageDataFormats.contains( ImageDataFormat.BdvOmeZarrS3 )
                || imageDataFormats.contains( ImageDataFormat.BdvN5S3 ) ||  imageDataFormats.contains( ImageDataFormat.OpenOrganelleS3 )))
					throw new UnsupportedOperationException( "Could not find any S3 storage of the images." );
			}
			else
			{
				if ( imageDataFormats.contains( ImageDataFormat.OmeZarr ) )
					settings.addImageDataFormat( ImageDataFormat.OmeZarr );
				if ( imageDataFormats.contains( ImageDataFormat.BdvOmeZarr ) )
					settings.addImageDataFormat( ImageDataFormat.BdvOmeZarr );
				if ( imageDataFormats.contains( ImageDataFormat.BdvN5 ) )
					settings.addImageDataFormat( ImageDataFormat.BdvN5 );
				else
					throw new UnsupportedOperationException( "Could not find any file system storage of the images." );
			}
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

		IJ.log( "Opened " + sourceNameToSourceAndConverters.size() + " image(s) in " + (System.currentTimeMillis() - startTime) + " ms, using up to " + MultiThreading.getNumIoThreads() + " thread(s).");

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
		viewManager = new ViewManager( this, userInterface, dataset.is2D );
		final View view = getView();
		view.setName( settings.values.getView() );
		IJ.log( "Opening view: " + view.getName() );
		final long startTime = System.currentTimeMillis();
		viewManager.show( view );
		IJ.log("Opened view: " + view.getName() + ", in " + (System.currentTimeMillis() - startTime) + " ms." );
	}

	private View getView() throws IOException
	{
		final View view = dataset.views.get( settings.values.getView() );
		if ( view == null )
		{
			System.err.println("The view \"" + settings.values.getView() + "\" could not be found in this dataset." );
			throw new IOException();
		}
		return view;
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
			IJ.log( "Closing I/O threads..." );
			MultiThreading.resetIOThreads();
			viewManager.close();
			IJ.log( "MoBIE closed." );
			IJ.log( "Closing MoBIE may have lead to errors due to processes that are interrupted." );
			IJ.log( "Usually it is fine to ignore those errors." );
		}
		catch ( Exception e )
		{
			IJ.log( "[ERROR] Could not fully close MoBIE." );
			e.printStackTrace();
		}

	}

	public synchronized ImageSource getSource( String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
	}

	public SourceAndConverter< ? > openSourceAndConverter( String sourceName, String log )
	{
		final ImageSource imageSource = getSource( sourceName );

		ImageDataFormat imageDataFormat = getImageDataFormat( sourceName, imageSource.imageData.keySet() );

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
			System.err.println( "Error or interruption opening: " + imagePath );
			throw e;
		}
    }

	private ImageDataFormat getImageDataFormat( String sourceName, Set< ImageDataFormat > sourceDataFormats )
	{
		for ( ImageDataFormat sourceDataFormat : sourceDataFormats )
		{
			if ( settings.values.getImageDataFormats().contains( sourceDataFormat ) )
			{
				return sourceDataFormat;
			}
		}

		System.err.println("Error opening: " + sourceName );
		for ( ImageDataFormat dataFormat : sourceDataFormats )
			System.err.println("Source supports: " + dataFormat);
		for ( ImageDataFormat dataFormat : settings.values.getImageDataFormats() )
			System.err.println("Project settings support: " + dataFormat);
		throw new RuntimeException();

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

	private List< Map< String, List< String > > > loadTables( Collection<String> sources, String tableName )
	{
		final List< Map< String, List< String > > > tables = new CopyOnWriteArrayList<>();

		final long start = System.currentTimeMillis();
		final ExecutorService executorService = MultiThreading.ioExecutorService;
		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( String sourceName : sources )
		{
			futures.add(
				executorService.submit( () -> {
					Map< String, List< String > > columns = TableHelper.loadTableAndAddImageIdColumn( sourceName, getTablePath( ( SegmentationSource ) getSource( sourceName ), tableName ) );
				tables.add( columns );
				} )
			);
		}
		MultiThreading.waitUntilFinished( futures );

		final long durationMillis = System.currentTimeMillis() - start;

		if ( durationMillis > minLogTimeMillis )
			IJ.log( "Read " + sources.size() + " table(s) in " + durationMillis + " ms, using up to " + MultiThreading.getNumIoThreads() + " thread(s).");

		return tables;
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
		IJ.log( "Read " + numTables + " table(s) in " + (System.currentTimeMillis() - startTimeMillis) + " ms, using up to " + MultiThreading.getNumIoThreads() + " thread(s).");
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

	private void mergeSegmentsTable( List< ? extends TableRow > tableRows, Map< String, List< String > > additionalTable )
	{
		// prepare
		final Map< String, List< String > > columnsForMerging = TableHelper.createColumnsForMerging( tableRows, additionalTable );

		// append
		for ( Map.Entry< String, List< String > > column : columnsForMerging.entrySet() )
		{
			TableRows.addColumn( tableRows, column.getKey(), column.getValue() );
		}
	}

	public void appendSegmentTableColumns( List< ? extends TableRow > tableRows, Collection< String > imageSourceNames, List< String > relativeTablePaths )
	{
		for ( String table : relativeTablePaths )
		{
			// load
			final List< Map< String, List< String > > > additionalTables = loadTables( imageSourceNames, table );

			// concatenate
			Map< String, List< String > > concatenatedTable = TableColumns.concatenate( additionalTables );

			// merge
			mergeSegmentsTable( tableRows, concatenatedTable );
		}
	}

	public void appendSegmentTableColumns( String source, String tablePath, List<TableRowImageSegment> tableRows )
	{
		// load
		Map< String, List< String > > additionalTable = TableHelper.loadTableAndAddImageIdColumn( source, tablePath );

		// merge
		mergeSegmentsTable( tableRows, additionalTable );
	}

	public void appendSegmentTableColumns( SegmentationDisplay segmentationDisplay, List< String > relativeTablePaths )
	{
		appendSegmentTableColumns( segmentationDisplay.tableRows, segmentationDisplay.getSources(), relativeTablePaths );
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
			TableHelper.appendRegionTableColumns( regionTableRows, additionalTables.get( i ) );
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

	private Map< String, String > getSegmentationTableDirectories( SegmentationDisplay display )
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
