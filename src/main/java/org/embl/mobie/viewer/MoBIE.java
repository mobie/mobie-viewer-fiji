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
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import org.embl.mobie.viewer.plugins.platybrowser.GeneSearchCommand;
import org.embl.mobie.viewer.serialize.Project;
import org.embl.mobie.viewer.serialize.SegmentationData;
import org.embl.mobie.viewer.serialize.Dataset;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ImageData;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.embl.mobie.viewer.source.SegmentationImage;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.SpimDataImage;
import org.embl.mobie.viewer.table.DefaultAnnData;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.TableHelper;
import org.embl.mobie.viewer.table.TableSawAnnotatedSegment;
import org.embl.mobie.viewer.table.TableSawAnnotatedSegmentTableModel;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewManager;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.ImgLoader;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.S3Utils;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
	private Map< String, Image< ? > > images;
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
		projectName = MoBIEHelper.getFileName( projectLocation );
		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
		project = new ProjectJsonParser().parseProject( IOHelper.combinePath( projectRoot,  "project.json" ) );
		setImageDataFormats( projectLocation );
		openDataset();
	}

	// TODO: Probably such Plugins should rather
	//  be provided by the MoBIESettings (or some annotation
	//  mechanism) such that additional commands
	//  could be registered without changing the core code.
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
		images = new ConcurrentHashMap< String, Image< ? > >();
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

	public synchronized ImageData getDataSource( String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
	}


	private List< TableRowImageSegment > tryOpenDefaultSegmentsTable( String sourceName )
	{
		try
		{
			return moBIE.loadImageSegmentsTable( sourceName, "default.tsv", "Open table: " );

		} catch ( Exception e )
		{
			// default table does not exist
			return null;
		}
	}

	private SourceAndConverter readSourceAndConverter( String sourceName, String log, ImageData imageData )
	{
		SourceAndConverter sourceAndConverter;
		ImageDataFormat imageDataFormat = getImageDataFormat( sourceName, imageData.imageData.keySet() );

		final String imagePath = getImagePath( imageData, imageDataFormat );
		if( log != null )
			IJ.log( log + imagePath );


		try
		{
			final long l = System.currentTimeMillis();
			SpimData spimData = tryOpenSpimData( imagePath, imageDataFormat );
			System.out.println("init setups: " + ( System.currentTimeMillis() - l ));
			sourceNameToImgLoader.put( sourceName, spimData.getSequenceDescription().getImgLoader() );

			final SourceAndConverterFromSpimDataCreator creator = new SourceAndConverterFromSpimDataCreator( spimData );
			sourceAndConverter = creator.getSetupIdToSourceAndConverter().values().iterator().next();

			// Initiate caches now such that the sources
			// are more interactive initially within BDV
			// TODO: it seems that with the optimisations in mobie-io
			//   https://github.com/mobie/mobie-io/pull/100
			//   this is not necessary anymore (takes only 1 ms) => remove?!
            final long l2 = System.currentTimeMillis();
			final int levels = sourceAndConverter.getSpimSource().getNumMipmapLevels();
			for ( int level = 0; level < levels; level++ )
				sourceAndConverter.getSpimSource().getSource( 0, level );
			System.out.println("init cache loaders: " + ( System.currentTimeMillis() - l2 ));
		}
		catch ( Exception e )
		{
			System.err.println( "Error or interruption opening: " + imagePath );
			throw e;
		}
		return sourceAndConverter;
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

    public Map<String, View > getViews()
    {
        return dataset.views;
    }

    private String getRelativeTableLocation( SegmentationData source )
    {
        return source.tableData.get( TableDataFormat.TabDelimitedFile ).relativePath;
    }

    public String getTableDirectory( SegmentationData source )
    {
        return getTableDirectory( getRelativeTableLocation( source ) );
    }

    public String getTableDirectory( String relativeTableLocation )
    {
        return IOHelper.combinePath( tableRoot, datasetName, relativeTableLocation );
    }


	public String getTablePath( SegmentationData source, String table )
	{
		return getTablePath( getRelativeTableLocation( source ), table );
	}

	public String getTablePath( String relativeTableLocation, String table )
	{
		return IOHelper.combinePath( tableRoot, getDatasetName(), relativeTableLocation, table );
	}

	public String getTableRootDirectory( String source )
	{
		final ImageData imageData = getDataSource( source );
		if ( imageData instanceof SegmentationData )
			return IOHelper.combinePath( tableRoot, getDatasetName(), getRelativeTableLocation( ( SegmentationData ) imageData ) );
		else
			throw new RuntimeException( "TODO: Implement getTableRootDirectory for " + imageData.getClass() );
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

	// TODO: probably we should move this functionality SegmentationDisplay!
	public List< TableRowImageSegment > loadImageSegmentsTable( String sourceName, String tableName, String log )
	{
		final SegmentationData tableSource = ( SegmentationData ) getDataSource( sourceName );
		final String tablePath = getTablePath( tableSource, tableName );
		if ( log != null )
			IJ.log( log + tablePath );
		final CsvReadOptions csvReadOptions = CsvReadOptions.builder( IOHelper.getReader( tablePath ) ).separator( '\t' ).build();
		final Table table = Table.read().usingOptions( csvReadOptions );
		final List< TableRowImageSegment > segments = MoBIEHelper.readImageSegmentsFromTableFile( tablePath, sourceName );
		return segments;
	}

	private List< Map< String, List< String > > > loadSegmentationTables( Collection< String > sources, String tableName )
	{
		final List< Map< String, List< String > > > tables = new CopyOnWriteArrayList<>();

		final long start = System.currentTimeMillis();
		final ExecutorService executorService = MultiThreading.ioExecutorService;
		final ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( String sourceName : sources )
		{
			futures.add(
				executorService.submit( () -> {
					Map< String, List< String > > columns = loadColumns( tableName, sourceName );
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

	public Map< String, List< String > > loadColumns( String tableName, String sourceName )
	{
		Map< String, List< String > > columns = TableHelper.loadTableAndAddImageIdColumn( sourceName, getTablePath( ( SegmentationData ) getDataSource( sourceName ), tableName ) );
		return columns;
	}

	private void appendSegmentsTableColumns( List< ? extends TableRow > tableRows, Map< String, List< String > > additionalTable )
	{
		// prepare
		final Map< String, List< String > > columnsForMerging = TableHelper.createColumnsForMerging( tableRows, additionalTable );

		// append
		for ( Map.Entry< String, List< String > > column : columnsForMerging.entrySet() )
		{
			TableRows.addColumn( tableRows, column.getKey(), column.getValue() );
		}
	}

	@Deprecated
	public void appendSegmentTableColumns( List< ? extends TableRow > tableRows, Collection< String > imageSourceNames, List< String > relativeTablePaths )
	{
		for ( String table : relativeTablePaths )
		{
			// load
			final List< Map< String, List< String > > > additionalTables = loadSegmentationTables( imageSourceNames, table );

			// concatenate
			Map< String, List< String > > concatenatedTable = TableColumns.concatenate( additionalTables );

			// merge
			appendSegmentsTableColumns( tableRows, concatenatedTable );
		}
	}

	public String getTableRoot()
	{
		return tableRoot;
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
		images.remove( sourceName );
		SourceAndConverterServices.getSourceAndConverterService().remove( sourceAndConverter );
	}

    public synchronized String getImagePath( ImageData source, ImageDataFormat imageDataFormat) {

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

	public void addImages( HashMap< String, Image< ? > > images )
	{
		this.images.putAll( images );
	}

	public ArrayList< String > getProjectCommands()
	{
		return projectCommands;
	}

	public HashMap< String, Image< ? > > initImages( Collection< String > sources )
	{
		final HashMap< String, Image< ? > > images = new HashMap<>();
		for ( String name : sources )
		{
			final ImageData imageData = getDataSource( name );
			ImageDataFormat imageDataFormat = getImageDataFormat( name, imageData.imageData.keySet() );
			final String imagePath = getImagePath( imageData, imageDataFormat );
			final SpimDataImage< ? > image = new SpimDataImage<>( imageDataFormat, imagePath, 0, name );

			if ( imageData.getClass() == SegmentationData.class )
			{
				final SegmentationData segmentationData = ( SegmentationData ) imageData;

				if ( segmentationData.tableData != null
						&& segmentationData.tableData.size() > 0 )
				{
					// Create image where pixel values
					// are the annotations and create
					// a table model for the annotations.
					final Set< String > columnPaths = getColumnPaths( segmentationData );
					final String defaultColumnsPath = columnPaths.stream().filter( p -> p.contains( "default" ) ).findFirst().get();

					final TableSawAnnotatedSegmentTableModel tableModel = new TableSawAnnotatedSegmentTableModel( defaultColumnsPath, image.getName() );
					tableModel.setColumnPaths( columnPaths );
					final DefaultAnnData< TableSawAnnotatedSegment > segmentsAnnData = new DefaultAnnData<>( tableModel );
					final SegmentationImage segmentationImage = new SegmentationImage( image, segmentsAnnData );
					images.put( name, segmentationImage );
				}
				else
				{
					// Label mask without annotation.
					// Create appropriate selection
					// and coloring model for the label
					// mask during display.
					images.put( name, image );
				}
			}
			else
			{
				// Intensity image.
				images.put( name, image );
			}
		}

		return images;
	}

	private Set< String > getColumnPaths( SegmentationData segmentationData )
	{
		final String tableDirectory = getTableDirectory( segmentationData );
		String[] fileNames = IOHelper.getFileNames( tableDirectory );
		final Set< String > columnPaths = new HashSet<>();
		for ( String fileName : fileNames )
		{
			columnPaths.add( IOHelper.combinePath( tableDirectory, fileName ) );
		}
		return columnPaths;
	}

	public Image< ? > getImage( String name )
	{
		return images.get( name );
	}
}
