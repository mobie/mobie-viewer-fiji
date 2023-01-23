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
import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imglib2.Dimensions;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.cmd.CmdHelper;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.github.GitHubUtils;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.viewer.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.viewer.color.ColorHelper;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.image.SpimDataImage;
import org.embl.mobie.viewer.image.SpotAnnotationImage;
import org.embl.mobie.viewer.plugins.platybrowser.GeneSearchCommand;
import org.embl.mobie.viewer.serialize.DataSource;
import org.embl.mobie.viewer.serialize.Dataset;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ImageDataSource;
import org.embl.mobie.viewer.serialize.Project;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.embl.mobie.viewer.serialize.RegionDataSource;
import org.embl.mobie.viewer.serialize.SegmentationDataSource;
import org.embl.mobie.viewer.serialize.SpotDataSource;
import org.embl.mobie.viewer.serialize.View;
import org.embl.mobie.viewer.serialize.display.ImageDisplay;
import org.embl.mobie.viewer.serialize.display.SegmentationDisplay;
import org.embl.mobie.viewer.io.StorageLocation;
import org.embl.mobie.viewer.table.DefaultAnnData;
import org.embl.mobie.viewer.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSegmentCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationTableModel;
import org.embl.mobie.viewer.table.saw.TableOpener;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.ViewManager;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MoBIE
{
	static
	{
		net.imagej.patcher.LegacyInjector.preinit();

		// Force TableSaw class loading and compilation to save time during the actual loading
		Table.read().usingOptions( CsvReadOptions.builderFromString( "aaa\tbbb" ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ) );

		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
	}

	private static MoBIE moBIE;
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";
	private MoBIESettings settings;
	private String currentDatasetName;
	private Dataset dataset;
	private ViewManager viewManager;
	private Project project;
	private UserInterface userInterface;
	private String projectRoot = "";
	private String imageRoot = ""; // see https://github.com/mobie/mobie-viewer-fiji/issues/933
	private String tableRoot = ""; // see https://github.com/mobie/mobie-viewer-fiji/issues/933
	private HashMap< String, ImgLoader > sourceNameToImgLoader;
	private ArrayList< String > projectCommands = new ArrayList<>();
	;
	public static int minLogTimeMillis = 100;
	public static boolean initiallyShowSourceNames = false;

	public MoBIE( String projectLocation ) throws IOException
	{
		this( projectLocation, new MoBIESettings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		settings.projectLocation( projectLocation );

		// Only allow one instance to avoid confusion
		if ( moBIE != null )
		{
			IJ.log("Detected running MoBIE instance.");
			moBIE.close();
		}
		moBIE = this;

		this.settings = settings;

		// Open project
		IJ.log("\n# MoBIE" );
		IJ.log("Opening project: " + projectLocation );
		setS3Credentials( settings );
		setProjectImageAndTableRootLocations();
		registerProjectPlugins( settings.values.getProjectLocation() );
		project = new ProjectJsonParser().parseProject( IOHelper.combinePath( projectRoot,  "project.json" ) );
		if ( project.getName() == null )
			project.setName( IOHelper.getFileName( projectLocation ) );
		setImageDataFormats( projectLocation );
		settings.addTableDataFormat( TableDataFormat.TSV );

		openAndViewDataset();
	}

	// use this constructor from the command line
	public MoBIE( String projectName, String[] imagePaths, String[] segmentationPaths, String[] tablePaths ) throws SpimDataException, IOException
	{
		initProject( projectName );

		if ( imagePaths.length == 1 && imagePaths[ 0 ].contains( "*" ) )
		{
			final String regExPath = imagePaths[ 0 ];

			final String dir = new File( regExPath ).getParent();
			String name = new File( regExPath ).getName();
			final String regex = CmdHelper.wildcardToRegex( name );

			final List< Path > pathList = Files.find( Paths.get( dir ), 999,
					( path, basicFileAttribute ) -> basicFileAttribute.isRegularFile()
							&& path.getFileName().toString().matches( regex ) ).collect( Collectors.toList() );
		}

		// images
		for ( String path : imagePaths )
		{
			final ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( path );
			final AbstractSpimData< ? > spimData = new SpimDataOpener().openSpimData( path, imageDataFormat );
			addSpimDataImages( spimData, false, null, null );
		}

		// segmentations (with tables)
		for ( int segmentationIndex = 0; segmentationIndex < segmentationPaths.length; segmentationIndex++ )
		{
			final String path = segmentationPaths[ segmentationIndex ];
			final ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( path );
			final AbstractSpimData< ? > spimData = new SpimDataOpener().openSpimData( path, imageDataFormat );

			if ( tablePaths != null && tablePaths.length > segmentationIndex )
			{
				// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/936
				final TableDataFormat tableDataFormat = TableDataFormat.fromPath( tablePaths[ segmentationIndex ] );
				final StorageLocation tableStorageLocation = new StorageLocation();
				final File tableFile = new File( tablePaths[ segmentationIndex ] );
				tableStorageLocation.absolutePath = tableFile.getParent();
				tableStorageLocation.defaultChunk = tableFile.getName();
				addSpimDataImages( spimData, true, tableStorageLocation, tableDataFormat );
			}
			else
			{
				addSpimDataImages( spimData, true, null, null );
			}
		}

		initUIandShowAllViews();
	}

	// use this constructor from the Fiji UI
	//
	// images: the idea is to convert all image data to {@code SpimData}
	// before calling this constructor.
	// {@code SpimDataOpener} in mobie-io provides methods for this.
	//
	// tables: one needs to provide the tableStorageLocation
	// and the tableDataFormat, which also specifies necessary column names.
	// note that this is more complex than for images, because we are not
	// aware of a good java implementation of tables
	// that would allow to wrap the various ways
	// in which tables can be stored, in particular considering lazy loading of
	// table chunks.
	public MoBIE( String projectName, AbstractSpimData< ? > image, AbstractSpimData< ? > segmentation, StorageLocation tableStorageLocation, TableDataFormat tableDataFormat )
	{
		initProject( projectName );

		addSpimDataImages( image, false, null, null );

		addSpimDataImages( segmentation, true, tableStorageLocation, tableDataFormat );

		initUIandShowAllViews();
	}

	private void initUIandShowAllViews()
	{
		initUI();
		for ( String viewName : dataset.views.keySet() )
			viewManager.show( getView( viewName, dataset ) );
	}

	private void initProject( String projectName )
	{
		// init settings, project and dataset
		settings = new MoBIESettings();
		project = new Project( projectName );
		currentDatasetName = project.getName();
		project.datasets().add( currentDatasetName );
		project.setDefaultDataset( currentDatasetName );
		// FIXME where is the link of a dataset to its name?
		dataset = new Dataset();
		dataset.is2D = true; // changed further down
	}

	private void addSpimDataImages(
			AbstractSpimData< ? > spimData,
			boolean isSegmentation,
			@Nullable StorageLocation tableStorageLocation, // for segmentations
			@Nullable TableDataFormat tableDataFormat // for segmentations
	)
	{
		final ImageDataFormat imageDataFormat = ImageDataFormat.SpimData;
		settings.addImageDataFormat( imageDataFormat );
		if ( tableDataFormat != null )
			settings.addTableDataFormat( tableDataFormat );

		final int numSetups = spimData.getSequenceDescription().getViewSetupsOrdered().size();

		for ( int setupIndex = 0; setupIndex < numSetups; setupIndex++ )
		{
			final StorageLocation storageLocation = new StorageLocation();
			storageLocation.data = spimData;
			storageLocation.channel = setupIndex;
			final String setupName = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupIndex ).getName();
			String imageName = getImageName( setupName, numSetups, setupIndex );

			DataSource dataSource;
			if ( isSegmentation )
			{
				dataSource = new SegmentationDataSource( imageName, imageDataFormat, storageLocation, tableDataFormat, tableStorageLocation );
				addSegmentationView( dataSource.getName(), spimData, setupIndex );
			}
			else
			{
				dataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
				addImageView( spimData, setupIndex, imageName );
			}

			dataSource.preInit( true );
			addDataSourceToDataset( spimData, setupIndex, dataSource );
		}
	}

	private void addImageView( AbstractSpimData< ? > spimData, int imageIndex, String imageName )
	{
		final Displaysettings displaysettings = spimData.getSequenceDescription().getViewSetupsOrdered().get( imageIndex ).getAttribute( Displaysettings.class );

		String color = "White";
		double[] contrastLimits = null;

		if ( displaysettings != null )
		{
			color = ColorHelper.getString( displaysettings.color );
			contrastLimits = new double[]{ displaysettings.min, displaysettings.max };
		}

		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, Arrays.asList( imageName ), color, contrastLimits, false, null );
		final View view = new View( imageName, "image", Arrays.asList( imageDisplay ), null, false );
		dataset.views.put( view.getName(), view );
	}

	private String getImageName( String imagePath, int numImages, int imageIndex )
	{
		String imageName = FilenameUtils.removeExtension( new File( imagePath ).getName() );
		if ( numImages > 1 )
			imageName += "_" + imageIndex;
		return imageName;
	}

	private void addSegmentationView( String imageName, AbstractSpimData< ? > spimData, int setupId  )
	{
		final SegmentationDisplay< ? > display = new SegmentationDisplay<>( imageName, Arrays.asList( imageName ) );

		final BasicViewSetup viewSetup = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupId );
		final double pixelWidth = viewSetup.getVoxelSize().dimension( 0 );
		display.setResolution3dView( new Double[]{ pixelWidth, pixelWidth, pixelWidth } );

		final View view = new View( imageName, "segmentation", Arrays.asList( display ), null, false );
		dataset.views.put( view.getName(), view );
	}

	private void addDataSourceToDataset( AbstractSpimData< ? > spimData, int setupId, DataSource dataSource )
	{
		dataset.sources.put( dataSource.getName(), dataSource );
		if ( dataset.is2D )
		{
			final Dimensions dimensions = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupId ).getSize();
			if ( dimensions.dimension( 2 ) > 1 )
				dataset.is2D = false;
		}
	}

	private StorageLocation configureCommandLineImageLocation( String imagePath, int channel, ImageDataFormat imageDataFormat )
	{
		final StorageLocation imageStorageLocation = new StorageLocation();
		imageStorageLocation.channel = channel;

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

	private void setImageDataFormats( String projectLocation )
	{
		final Set< ImageDataFormat > imageDataFormat = settings.values.getImageDataFormats();

		if ( imageDataFormat.size() == 0 )
		{
			if ( projectLocation.startsWith( "http" ) )
			{
				 settings.addImageDataFormat( ImageDataFormat.OmeZarrS3 );
				 settings.addImageDataFormat( ImageDataFormat.BdvOmeZarrS3 );
				 settings.addImageDataFormat( ImageDataFormat.BdvN5S3 );
				 settings.addImageDataFormat( ImageDataFormat.OpenOrganelleS3 );
			}
			else
			{
				settings.addImageDataFormat( ImageDataFormat.OmeZarr );
				settings.addImageDataFormat( ImageDataFormat.BdvOmeZarr );
				settings.addImageDataFormat( ImageDataFormat.BdvN5 );
				settings.addImageDataFormat( ImageDataFormat.BdvHDF5 );
			}
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
		// read dataset from file
		IJ.log("Opening dataset: " + datasetName );
		setCurrentDatasetName( datasetName );
		dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );

		// set data source names
		for ( Map.Entry< String, DataSource > entry : dataset.sources.entrySet() )
			entry.getValue().setName( entry.getKey() );

		// log views
		System.out.println("# Available views");
		for ( String s : getViews().keySet() )
			System.out.println( s );
		System.out.println("/n");

		// build UI and show view
		initUI();
		viewManager.show( getView( viewName, dataset ) );
	}

	private void initUI()
	{
		WindowArrangementHelper.setLogWindowPositionAndSize();
		sourceNameToImgLoader = new HashMap<>();
		userInterface = new UserInterface( this );
		viewManager = new ViewManager( this, userInterface, dataset.is2D );
	}

	private View getView( String viewName, Dataset dataset )
	{
		final View view = dataset.views.get( viewName );
		if ( view == null )
			throw new UnsupportedOperationException("The view \"" + viewName + "\" does not exist in the current dataset." );
		view.setName( viewName );
		return view;
	}

	private void setCurrentDatasetName( String datasetName )
	{
		this.currentDatasetName = datasetName;
	}

	private String createPath( String rootLocation, String githubBranch, String... files )
	{
		if ( rootLocation.contains( "github.com" ) )
		{
			rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
		}

		final ArrayList< String > strings = new ArrayList<>();
		strings.add( rootLocation );
		Collections.addAll( strings, files );
		final String path = IOHelper.combinePath( strings.toArray( new String[0] ) );

		return path;
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

	public String getCurrentDatasetName()
	{
		return currentDatasetName;
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
			IJ.log( "Closing MoBIE may have lead to errors due to processes that are interrupted." );
			IJ.log( "Usually it is fine to ignore those errors." );
		}
		catch ( Exception e )
		{
			IJ.log( "[ERROR] Could not fully close MoBIE." );
			e.printStackTrace();
		}

	}

	public synchronized DataSource getData( String sourceName )
	{
		return dataset.sources.get( sourceName );
	}

	private ImageDataFormat getImageDataFormat( ImageDataSource imageSource )
	{
		final Set< ImageDataFormat > imageDataFormats = settings.values.getImageDataFormats();

		for ( ImageDataFormat dataFormat : imageSource.imageData.keySet() )
		{
			if ( imageDataFormats.contains( dataFormat ) )
			{
				// TODO (discuss with Constantin)
				//   it is weird that it just returns the first one...
				return dataFormat;
			}
		}

		System.err.println("Error opening: " + imageSource.getName() );
		for ( ImageDataFormat dataFormat : imageSource.imageData.keySet() )
			System.err.println("Source supports: " + dataFormat);
		for ( ImageDataFormat dataFormat : imageDataFormats )
			System.err.println("Settings support: " + dataFormat);

		throw new RuntimeException();
	}

	public TableDataFormat getTableFormat( Map< TableDataFormat, StorageLocation > tableData )
	{
		final Set< TableDataFormat > tableDataFormats = settings.values.getTableDataFormats();

		for ( TableDataFormat dataFormat : tableData.keySet() )
		{
			if ( tableDataFormats.contains( dataFormat ) )
			{
				// TODO (discuss with Constantin)
				//   it is weird that it just returns the first one...
				return dataFormat;
			}
		}

		System.err.println("Error opening table.");
		for ( TableDataFormat dataFormat : tableData.keySet() )
			System.err.println("Source supports: " + dataFormat);
		for ( TableDataFormat dataFormat : tableDataFormats )
			System.err.println("Settings support: " + dataFormat);

		throw new RuntimeException();
	}

	public void setDataset( String dataset )
    {
        setCurrentDatasetName( dataset );
        viewManager.close();

        try {
            openAndViewDataset( dataset, View.DEFAULT );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public Map< String, View > getViews()
    {
        return dataset.views;
    }

	// equivalent to {@code getImageLocation}
    public StorageLocation getTableLocation( Map< TableDataFormat, StorageLocation > tableData )
    {
		final TableDataFormat tableDataFormat = getTableFormat( tableData );
		final StorageLocation storageLocation = tableData.get( tableDataFormat );

		if ( storageLocation.relativePath != null )
		{
			storageLocation.defaultChunk = TableDataFormat.MOBIE_DEFAULT_CHUNK;
			storageLocation.absolutePath = IOHelper.combinePath( tableRoot, currentDatasetName, storageLocation.relativePath );
			return storageLocation;
		}

		return storageLocation;
    }

	public String getDatasetPath( String... files )
	{
		final String datasetRoot = IOHelper.combinePath( projectRoot, getCurrentDatasetName() );
		return createPath( datasetRoot, files );
	}

	private String createPath( String root, String[] files )
	{
		String location = root;
		for ( String file : files )
			location = IOHelper.combinePath( location, file );
		return location;
	}

	@Deprecated
	// delegate to BDV-PL
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

	// equivalent to {@code getTableLocation}
    public synchronized String getImageLocation( ImageDataFormat imageDataFormat, StorageLocation storageLocation )
	{
		switch (imageDataFormat) {
			case BioFormats:
			case BdvHDF5:
			case BdvN5:
			case BdvOmeZarr:
			case BdvOmeZarrS3: // assuming that the xml is not at storageLocation.s3Address
			case BdvN5S3: // assuming that the xml is not at storageLocation.s3Address
			case OmeZarr:
            	if ( storageLocation.absolutePath != null  )
					return storageLocation.absolutePath;
                return IOHelper.combinePath( imageRoot, getCurrentDatasetName(), storageLocation.relativePath );
            case OpenOrganelleS3:
            case OmeZarrS3:
                return storageLocation.s3Address;
            default:
                throw new UnsupportedOperationException( "File format not supported: " + imageDataFormat );
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
			// FIXME Cache SpimData?
			//   https://github.com/mobie/mobie-viewer-fiji/issues/857
			// FIXME This currently only is used for region tables,
			//   and thus seems to be of no general use
			//   also consider:
			if ( DataStore.containsRawData( dataSource.getName() ) )
			{
				continue;
			}

			futures.add(
				ThreadHelper.ioExecutorService.submit( () ->
					{
						String log = getLog( sourceIndex, numImages, sourceLoggingModulo, lastLogMillis );
						try
						{
							initDataSource( dataSource, log );
						}
						catch ( SpimDataException e )
						{
							e.printStackTrace();
						}
					}
				) );
		}

		ThreadHelper.waitUntilFinished( futures );
		IJ.log( "Initialised " + dataSources.size() + " data source(s) in " + (System.currentTimeMillis() - startTime) + " ms, using up to " + ThreadHelper.getNumIoThreads() + " thread(s).");
	}

	private void initDataSource( DataSource dataSource, String log ) throws SpimDataException
	{
		if ( dataSource instanceof ImageDataSource )
		{
			final ImageDataSource imageSource = ( ImageDataSource ) dataSource;
			final ImageDataFormat imageDataFormat = getImageDataFormat( imageSource );
			final StorageLocation storageLocation = imageSource.imageData.get( imageDataFormat );
			final Integer channel = storageLocation.channel;
			final Image< ? > image = initImage( imageDataFormat, channel, storageLocation, imageSource.getName() );

			if ( dataSource.preInit() )
			{
				// force initialization here to save time later
				final Source source = image.getSourcePair().getSource();
				final int levels = source.getNumMipmapLevels();
				for ( int level = 0; level < levels; level++ )
					source.getSource( 0, level ).randomAccess();
			}

			if ( dataSource.getClass() == SegmentationDataSource.class )
			{
				final SegmentationDataSource segmentationDataSource = ( SegmentationDataSource ) dataSource;

				if ( segmentationDataSource.tableData != null )
				{
					TableSawAnnotationTableModel< TableSawAnnotatedSegment > tableModel = createTableModel( segmentationDataSource );

					final DefaultAnnData< TableSawAnnotatedSegment > annData = new DefaultAnnData<>( tableModel );

					final DefaultAnnotationAdapter< TableSawAnnotatedSegment > annotationAdapter = new DefaultAnnotationAdapter( annData );

					final AnnotatedLabelImage< TableSawAnnotatedSegment > annotatedLabelImage = new DefaultAnnotatedLabelImage( image, annData, annotationAdapter );

					// label image representing annotated segments
					DataStore.putImage( annotatedLabelImage );
				}
				else
				{
					// label image representing segments
					// without annotation table
					final LazyAnnotatedSegmentTableModel tableModel = new LazyAnnotatedSegmentTableModel( image.getName() );
					final DefaultAnnData< AnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
					final LazyAnnotatedSegmentAdapter segmentAdapter = new LazyAnnotatedSegmentAdapter( image.getName(), tableModel );
					final DefaultAnnotatedLabelImage annotatedLabelImage = new DefaultAnnotatedLabelImage( image, annData, segmentAdapter );
					DataStore.putImage( annotatedLabelImage );
				}
			}
			else
			{
				// intensity image
				DataStore.putImage( image );
			}
		}

		if ( dataSource instanceof SpotDataSource )
		{
			//final long start = System.currentTimeMillis();
			final SpotDataSource spotDataSource = ( SpotDataSource ) dataSource;
			final StorageLocation tableLocation = getTableLocation( spotDataSource.tableData );
			final TableDataFormat tableFormat = getTableFormat( spotDataSource.tableData );

			Table table = TableOpener.open( tableLocation, tableFormat );

			final TableSawAnnotationCreator< TableSawAnnotatedSpot > annotationCreator = new TableSawAnnotatedSpotCreator( table );

			final TableSawAnnotationTableModel< AnnotatedSpot > tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableLocation, tableFormat, table );

			final DefaultAnnData< AnnotatedSpot > spotAnnData = new DefaultAnnData<>( tableModel );

			final SpotAnnotationImage< AnnotatedSpot > spotAnnotationImage = new SpotAnnotationImage( spotDataSource.getName(), spotAnnData, 1.0, spotDataSource.boundingBoxMin, spotDataSource.boundingBoxMax );

			// Spots image, built from spots table
			DataStore.putImage( spotAnnotationImage );

			// System.out.println("Created spots image " + spotsImage.getName() + " with " + spotAnnData.getTable().numAnnotations() + " spots in [ms] " + ( System.currentTimeMillis() - start ));
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
			final TableDataFormat tableFormat = getTableFormat( regionDataSource.tableData );

			regionDataSource.table = TableOpener.open( tableLocation, tableFormat );

			DataStore.putRawData( regionDataSource );
		}

		if ( log != null )
			IJ.log( log + dataSource.getName() );
	}

	private TableSawAnnotationTableModel< TableSawAnnotatedSegment > createTableModel( SegmentationDataSource dataSource )
	{
		final StorageLocation tableLocation = getTableLocation( dataSource.tableData );
		final TableDataFormat tableFormat = getTableFormat( dataSource.tableData );

		Table table = dataSource.preInit() ? TableOpener.open( tableLocation, tableFormat ) : null;

		final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( null, table );

		final TableSawAnnotationTableModel tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableLocation, tableFormat, table );

		return tableModel;
	}

	private SpimDataImage< ? > initImage( ImageDataFormat imageDataFormat, Integer channel, StorageLocation storageLocation, String name )
	{
		switch ( imageDataFormat )
		{
			case SpimData:
				return new SpimDataImage<>( ( AbstractSpimData ) storageLocation.data, channel, name );
			default:
				// TODO https://github.com/mobie/mobie-viewer-fiji/issues/857
				final String imagePath = getImageLocation( imageDataFormat, storageLocation );
				return new SpimDataImage( imageDataFormat, imagePath, channel, name, ThreadHelper.sharedQueue );
		}
	}

	public List< DataSource > getDataSources( Set< String > names )
	{
		return names.stream()
				.filter( name -> ( dataset.sources.containsKey( name ) ) )
				.map( s -> dataset.sources.get( s ) )
				.collect( Collectors.toList() );
	}
}
