/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie;

import bdv.img.n5.N5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.WindowManager;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.lib.*;
import org.embl.mobie.lib.files.ImageFileSources;
import org.embl.mobie.lib.files.FileSourcesDataSetter;
import org.embl.mobie.lib.files.LabelFileSources;
import org.embl.mobie.lib.files.SourcesFromPathsCreator;
import org.embl.mobie.lib.hcs.HCSDataAdder;
import org.embl.mobie.lib.hcs.Plate;
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.image.CachedCellImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.SpimDataImage;
import org.embl.mobie.lib.io.DataFormats;
import org.embl.mobie.lib.io.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.plugins.platybrowser.GeneSearchCommand;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.Project;
import org.embl.mobie.lib.serialize.ProjectJsonParser;
import org.embl.mobie.lib.serialize.RegionDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.SpotDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.lib.ui.UserInterface;
import org.embl.mobie.lib.ui.WindowArrangementHelper;
import org.embl.mobie.lib.view.ViewManager;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.embl.mobie.io.util.IOHelper.combinePath;
import static org.embl.mobie.io.util.IOHelper.getFileName;

public class MoBIE
{
	static
	{
		net.imagej.patcher.LegacyInjector.preinit();
		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
	}

	private static MoBIE moBIE;
	public static ImageJ imageJ;

	private String projectLocation;
	private MoBIESettings settings;
	private Project project;
	private Dataset dataset;
	private String projectRoot = "";
	private String imageRoot = "";
	private String tableRoot = "";

	private ViewManager viewManager;
	private UserInterface userInterface;
	private HashMap< String, ImgLoader > sourceNameToImgLoader;
	private final ArrayList< String > projectCommands = new ArrayList<>();

	public MoBIE( String projectLocation ) throws IOException
	{
		this( projectLocation, new MoBIESettings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		this.settings = settings;
		this.projectLocation = projectLocation;

		initTableSaw();

		initImageJAndMoBIE();

		IJ.log("\n# MoBIE" );
		IJ.log("Opening: " + projectLocation );

		openMoBIEProject();
	}

	public static MoBIE getInstance()
	{
		return moBIE;
	}

	public MoBIE( String hcsDataLocation, MoBIESettings settings, double relativeWellMargin, double relativeSiteMargin ) throws IOException
	{
		this.settings = settings;
		this.projectLocation = hcsDataLocation;

		initImageJAndMoBIE();

		IJ.log("\n# MoBIE" );
		IJ.log("Opening: " + hcsDataLocation );

		openHCSDataset( relativeWellMargin, relativeSiteMargin );
	}

	public MoBIE( List< String > imagePaths, List< String > labelPaths, List< String > labelTablePaths, String root, GridType grid, MoBIESettings settings ) throws IOException
	{
		this.settings = settings;

		System.out.println( "root: " + root );
		System.out.println( "images: " + Arrays.toString( imagePaths.toArray() ) );
		System.out.println( "labels: " + Arrays.toString( labelPaths.toArray() ) );
		System.out.println( "tables: " + Arrays.toString( labelTablePaths.toArray() ) );

		final SourcesFromPathsCreator sourcesCreator = new SourcesFromPathsCreator( imagePaths, labelPaths, labelTablePaths, root, grid );

		final List< ImageFileSources > imageFileSources = sourcesCreator.getImageSources();
		final List< LabelFileSources > labelSources = sourcesCreator.getLabelSources();

		openImagesAndLabels( imageFileSources, labelSources );
	}

	// open an image or object table
	public MoBIE( String tablePath, List< String > imageColumns, List< String > labelColumns, String root, GridType grid, MoBIESettings settings ) throws IOException
	{
		this.settings = settings;

		final SourcesFromTableCreator sourcesCreator = new SourcesFromTableCreator( tablePath, imageColumns, labelColumns, root, grid );

		final List< ImageFileSources > imageSources = sourcesCreator.getImageSources();
		final List< LabelFileSources > labelSources = sourcesCreator.getLabelSources();

		openImagesAndLabels( imageSources, labelSources );
	}

	private void initTableSaw()
	{
		// force TableSaw class loading
		// to save time during the actual loading
		// TODO: this makes no sense if we don't open a project with tables
		Table.read().usingOptions( CsvReadOptions.builderFromString( "aaa\tbbb" ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ) );
	}

	private void openMoBIEProject() throws IOException
	{
		setS3Credentials( settings );
		setProjectImageAndTableRootLocations();
		registerProjectPlugins( projectLocation );
		project = new ProjectJsonParser().parseProject( combinePath( projectRoot, "project.json" ) );
		if ( project.getName() == null ) project.setName( getFileName( projectLocation ) );
		settings.addTableDataFormat( TableDataFormat.TSV );
		openAndViewDataset();
	}

	// TODO 2D or 3D?
	private void openImagesAndLabels( List< ImageFileSources > images, List< LabelFileSources > labels )
	{
		initImageJAndMoBIE();

		initProject( "" );

		new FileSourcesDataSetter( images, labels ).addData( dataset );

		initUIandShowView( dataset.views().keySet().iterator().next() );
	}

	public MoBIE( String projectName, AbstractSpimData< ? > image, @Nullable AbstractSpimData< ? > labels, @Nullable StorageLocation tableStorageLocation, @Nullable TableDataFormat tableDataFormat )
	{
		settings = new MoBIESettings();

		initImageJAndMoBIE();

		initProject( projectName );

		final SpimDataAdder spimDataAdder = new SpimDataAdder( image, labels, tableStorageLocation, tableDataFormat );

		// TODO: Do I really need the settings here?
		spimDataAdder.addData( dataset, settings );

		initUIandShowView( null );
	}

	private void initImageJAndMoBIE()
	{
		DebugTools.setRootLevel( "OFF" ); // Disable Bio-Formats logging

		if ( settings.values.isOpenedFromCLI() )
		{
			// TODO: if possible open init the SciJava Services
			//   by different means
			imageJ = new ImageJ(); // Init SciJava Services
			imageJ.ui().showUI(); // Enable SciJava Command rendering
		}

		if ( moBIE != null )
		{
			// only allow one instance to avoid confusion
			IJ.log("Detected running MoBIE instance.");
			moBIE.close();
		}

		moBIE = this;
	}

	private void openHCSDataset( double wellMargin, double siteMargin ) throws IOException
	{
		initProject( "HCS" );
		final Plate plate = new Plate( projectLocation );
		IJ.log( "HCS Pattern: " + plate.getHcsPattern() );
		new HCSDataAdder( plate, wellMargin, siteMargin ).addData( dataset );
		initUIandShowView( dataset.views().keySet().iterator().next() );
	}

	private void initUIandShowView( @Nullable String view )
	{
		buildUI();

		if ( view == null )
		{
			// show all views
			for ( String viewName : getViews().keySet() )
				viewManager.show( getView( viewName, dataset ) );
		}
		else
		{
			viewManager.show( getView( view, dataset ) );
		}
	}

	private void adjustLogWindow( UserInterface userInterface )
	{
		final Window userInterfaceWindow = userInterface.getWindow();
		IJ.log( " " ); // ensures that the window exists
		WindowArrangementHelper.bottomAlignWindow( userInterfaceWindow, WindowManager.getWindow( "Log" ), true, true );
	}

	// use this if there is no project.json
	private void initProject( String projectName )
	{
		if ( settings == null ) settings = new MoBIESettings();
		project = new Project( projectName );
		dataset = new Dataset( project.getName() );
		project.datasets().add( dataset.getName() );
		project.setDefaultDataset( dataset.getName() );
		dataset.is2D( true );
	}

	public String getProjectLocation()
	{
		return projectLocation;
	}

	public Project getProject()
	{
		return project;
	}

	// TODO not used?!
	private StorageLocation configureCommandLineImageLocation( String imagePath, int channel, ImageDataFormat imageDataFormat )
	{
		final StorageLocation imageStorageLocation = new StorageLocation();
		imageStorageLocation.setChannel( channel );

		if ( imageDataFormat.isRemote() )
		{
			imageStorageLocation.s3Address = imagePath;
			return imageStorageLocation;
		}

		imageStorageLocation.absolutePath = imagePath;
		return imageStorageLocation;
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

	private void openAndViewDataset() throws IOException
	{
		if ( settings.values.getDataset() != null )
		{
			openAndViewDataset( settings.values.getDataset(), settings.values.getView() );
		}
		else
		{
			openAndViewDataset( project.getDefaultDataset(), settings.values.getView() );
		}
	}

	private void setProjectImageAndTableRootLocations( )
	{
		projectRoot = IOHelper.createPath( projectLocation, settings.values.getProjectBranch() );

		if( ! org.embl.mobie.io.util.IOHelper.exists( combinePath( projectRoot, "project.json" ) ) )
		{
			projectRoot = combinePath( projectRoot, "data" );
		}

		imageRoot = IOHelper.createPath(
				settings.values.getImageDataLocation() != null ? settings.values.getImageDataLocation() : projectLocation,
				settings.values.getImageDataBranch() );

		if( ! org.embl.mobie.io.util.IOHelper.exists( combinePath( imageRoot, "project.json" ) ) )
		{
			imageRoot = combinePath( imageRoot, "data" );
		}

		tableRoot = IOHelper.createPath(
				settings.values.getTableDataLocation() != null ? settings.values.getTableDataLocation() : projectLocation,
				settings.values.getTableDataBranch() );

		if( ! org.embl.mobie.io.util.IOHelper.exists( combinePath( tableRoot, "project.json" ) ) )
		{
			tableRoot = combinePath( tableRoot, "data" );
		}
	}

	private String getLog( AtomicInteger dataSetIndex, int numTotal, AtomicInteger dataSetLoggingInterval, AtomicLong lastLogMillis )
	{
		final int currentDatasetIndex = dataSetIndex.incrementAndGet();

		if ( currentDatasetIndex % dataSetLoggingInterval.get() == 0  )
		{
			// Update logging frequency
			// such that a message appears
			// approximately every 5000 ms
			final long currentTimeMillis = System.currentTimeMillis();
			if ( currentTimeMillis - lastLogMillis.get() < 4000 )
				dataSetLoggingInterval.set( Math.max( 1, dataSetLoggingInterval.get() * 2 ) );
			else if ( currentTimeMillis - lastLogMillis.get() > 6000 )
				dataSetLoggingInterval.set( Math.max( 1, dataSetLoggingInterval.get() / 2  ) );
			lastLogMillis.set( currentTimeMillis );

			// Return log message
			return "Initialising (" + currentDatasetIndex + "/" + numTotal + "): ";
		}
		else
		{
			return null;
		}
	}

	private void openAndViewDataset( String datasetName, String viewName ) throws IOException
	{
		IJ.log("Opening dataset: " + datasetName );
		final String datasetJsonPath = combinePath( projectRoot, datasetName, "dataset.json" );
		dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
		dataset.setName( datasetName );

		// set data source names
		for ( Map.Entry< String, DataSource > entry : dataset.sources().entrySet() )
			entry.getValue().setName( entry.getKey() );

		// log views
		System.out.println("# Available views");
		for ( String view : getViews().keySet() )
			System.out.println( view );

		// build UI and show view
		buildUI();
		viewManager.show( getView( viewName, dataset ) );
	}

	private void buildUI()
	{
		sourceNameToImgLoader = new HashMap<>();
		userInterface = new UserInterface( this );
		adjustLogWindow( userInterface );
		viewManager = new ViewManager( this, userInterface, dataset.is2D() );
	}

	private View getView( String viewName, Dataset dataset )
	{
		final View view = dataset.views().get( viewName );
		if ( view == null )
			throw new UnsupportedOperationException("The view \"" + viewName + "\" does not exist in the current dataset." );
		view.setName( viewName );
		return view;
	}

	public ViewManager getViewManager()
	{
		return viewManager;
	}

	public String getProjectName()
	{
		return project.getName();
	}

	public MoBIESettings getSettings()
	{
		return settings;
	}

	public Dataset getDataset()
	{
		return dataset;
	}

	public List< String > getDatasets()
	{
		if ( project == null ) return null;
		return project.datasets();
	}

	public UserInterface getUserInterface() { return userInterface; }

	public void close()
	{
		try
		{
			IJ.log( "Closing MoBIE..." );
			IJ.log( "Closing I/O threads..." );
			ThreadHelper.resetIOThreads();
			viewManager.close();
			IJ.log( "MoBIE closed." );
			if ( settings.values.isOpenedFromCLI() )
				System.exit( 0 );
		}
		catch ( RuntimeException e )
		{
			IJ.log( "[ERROR] Could not fully close MoBIE." );
			e.printStackTrace();
			if ( settings.values.isOpenedFromCLI() )
				System.exit( 1 );
		}
	}

	private ImageDataFormat getImageDataFormat( ImageDataSource imageSource )
	{
		final List< ImageDataFormat > formats = DataFormats.getImageDataFormats( settings.values.getPreferentialLocation() );

		// The {@code formats} contain all supported image data formats sorted in
		// order of preference. This preference is set by the user when opening the project.
		for ( ImageDataFormat format : formats )
		{
			if ( imageSource.imageData.keySet().contains( format ) )
			{
				return format;
			}
		}

		throw new RuntimeException( "Could not find a storage location for: " + imageSource.getName() );
	}

	public TableDataFormat getTableDataFormat( Map< TableDataFormat, StorageLocation > tableData )
	{
		final Set< TableDataFormat > settingsFormats = settings.values.getTableDataFormats();

		if ( settingsFormats.size() == 0 )
		{
			/*
				there is no preferred table data format specified,
				thus we simply use the first (and potentially only) one
			 */
			return tableData.keySet().iterator().next();
		}


		for ( TableDataFormat sourceFormat : tableData.keySet() )
		{
			if ( settingsFormats.contains( sourceFormat ) )
			{
				/*
					return the first source format that is
					specified by the settings
			 	*/
				return sourceFormat;
			}
		}

		System.err.println( "Error opening table." );
		for ( TableDataFormat dataFormat : tableData.keySet() )
			System.err.println( "Source supports: " + dataFormat );
		for ( TableDataFormat dataFormat : settingsFormats )
			System.err.println( "Settings support: " + dataFormat );
		throw new RuntimeException( "Error determining the table data format." );
	}

	public void setDataset( String datasetName )
    {
        viewManager.close();

        try {
            openAndViewDataset( datasetName, View.DEFAULT );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public Map< String, View > getViews()
    {
		for ( String name : dataset.views().keySet() )
			dataset.views().get( name ).setName( name );

		return dataset.views();
    }

	// equivalent to {@code getImageLocation}
    public StorageLocation getTableLocation( Map< TableDataFormat, StorageLocation > tableData )
    {
		final TableDataFormat tableDataFormat = getTableDataFormat( tableData );
		final StorageLocation storageLocation = tableData.get( tableDataFormat );

		if ( storageLocation.relativePath != null )
		{
			storageLocation.defaultChunk = TableDataFormat.MOBIE_DEFAULT_CHUNK;
			storageLocation.absolutePath = combinePath( tableRoot, dataset.getName(), storageLocation.relativePath );
			return storageLocation;
		}

		return storageLocation;
    }

	public String absolutePath( String... files )
	{
		String location = combinePath( projectRoot, getDataset().getName() );
		for ( String file : files )
			location = combinePath( location, file );
		return location;
	}

	@Deprecated
	// TODO https://github.com/bigdataviewer/bigdataviewer-playground/issues/259#issuecomment-1279705489
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
		SourceAndConverterServices.getSourceAndConverterService().remove( sourceAndConverter );
	}

    public synchronized String getImageLocation( ImageDataFormat imageDataFormat, StorageLocation storageLocation )
	{
		switch (imageDataFormat) {
			case OpenOrganelleS3:
            case OmeZarrS3:
                return storageLocation.s3Address;
            default:
				if ( storageLocation.absolutePath != null  )
					return storageLocation.absolutePath;
				else
					return combinePath( imageRoot, dataset.getName(), storageLocation.relativePath );
        }
    }

	public ArrayList< String > getProjectCommands()
	{
		return projectCommands;
	}

	public void initDataSources( Collection< DataSource > dataSources )
	{
		IJ.log("Initializing data from " + dataSources.size() + " source(s)..." );

		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		AtomicInteger sourceIndex = new AtomicInteger(0);
		final int numImages = dataSources.size();

		AtomicInteger sourceLoggingModulo = new AtomicInteger(1);
		AtomicLong lastLogMillis = new AtomicLong( System.currentTimeMillis() );
		final long startTime = System.currentTimeMillis();

		for ( DataSource dataSource : dataSources )
		{
			// TODO Cache SpimData?
			//   https://github.com/mobie/mobie-viewer-fiji/issues/857
			// TODO This currently only is used for region tables,
			//   and thus seems to be of no general use
			if ( DataStore.containsRawData( dataSource.getName() ) )
			{
				continue;
			}

			futures.add(
				ThreadHelper.ioExecutorService.submit( () ->
					{
						String log = getLog( sourceIndex, numImages, sourceLoggingModulo, lastLogMillis );
						initDataSource( dataSource, log );
					}
				) );
		}

		ThreadHelper.waitUntilFinished( futures );
		IJ.log( "Initialised " + dataSources.size() + " data source(s) in " + (System.currentTimeMillis() - startTime) + " ms, using up to " + ThreadHelper.getNumIoThreads() + " thread(s).");
	}

	private void initDataSource( DataSource dataSource, String log )
	{
		if ( dataSource instanceof ImageDataSource )
		{
			final ImageDataSource imageSource = ( ImageDataSource ) dataSource;
			final ImageDataFormat imageDataFormat = getImageDataFormat( imageSource );
			final StorageLocation storageLocation = imageSource.imageData.get( imageDataFormat );
			final Image< ? > image = initImage( imageDataFormat, storageLocation, imageSource.getName() );

			if ( dataSource.preInit() )
			{
				// force initialization here to save time later
				// (i.e. help smooth rendering in BDV)
				final Source< ? > source = image.getSourcePair().getSource();
				final int levels = source.getNumMipmapLevels();
				for ( int level = 0; level < levels; level++ )
					source.getSource( 0, level ).randomAccess();
			}

			if ( dataSource.getClass() == SegmentationDataSource.class )
			{
				// label image
				final AnnotatedLabelImageCreator labelImageCreator = new AnnotatedLabelImageCreator( this, ( SegmentationDataSource ) dataSource, image );
				DataStore.putImage( labelImageCreator.create() );
			}
			else
			{
				// intensity image
				DataStore.putImage( image );
			}
		}

		if ( dataSource instanceof SpotDataSource )
		{
			// build spots image from spots table
			final SpotImageCreator spotImageCreator = new SpotImageCreator( ( SpotDataSource ) dataSource, this );
			DataStore.putImage( spotImageCreator.create() );
		}

		if ( dataSource instanceof RegionDataSource )
		{
			// Region images cannot be fully initialised
			// here because the region annotations can refer
			// to images that are created later by means of a
			// transformation.
			// However, we can already load the region table here.
			final RegionDataSource regionDataSource = ( RegionDataSource ) dataSource;
			final StorageLocation tableLocation = getTableLocation( regionDataSource.tableData );
			final TableDataFormat tableFormat = getTableDataFormat( regionDataSource.tableData );
			regionDataSource.table = TableOpener.open( tableLocation, tableFormat );
			DataStore.putRawData( regionDataSource );
		}

		if ( log != null )
			IJ.log( log + dataSource.getName() );
	}


	private Image< ? > initImage( ImageDataFormat imageDataFormat, StorageLocation storageLocation, String name )
	{
		if ( imageDataFormat.equals( ImageDataFormat.SpimData ) )
		{
			return new SpimDataImage<>( ( AbstractSpimData ) storageLocation.data, storageLocation.getChannel(), name, settings.values.getRemoveSpatialCalibration() );
		}

		if ( imageDataFormat.equals( ImageDataFormat.IlastikHDF5 ) )
		{
			return new CachedCellImage<>( name, storageLocation.absolutePath, storageLocation.getChannel(), imageDataFormat, ThreadHelper.sharedQueue );
		}

		if ( storageLocation instanceof Site )
		{
			return new SpimDataImage( ( Site ) storageLocation, name, ThreadHelper.sharedQueue );
		}

		final String imagePath = getImageLocation( imageDataFormat, storageLocation );
		final SpimDataImage spimDataImage = new SpimDataImage( imageDataFormat, imagePath, storageLocation.getChannel(), name, ThreadHelper.sharedQueue, settings.values.getRemoveSpatialCalibration() );
		return spimDataImage;
	}

	public List< DataSource > getDataSources( Set< String > names )
	{
		return names.stream()
				.filter( name -> ( dataset.sources().containsKey( name ) ) )
				.map( s -> dataset.sources().get( s ) )
				.collect( Collectors.toList() );
	}
}
