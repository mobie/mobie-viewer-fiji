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
package org.embl.mobie;

import bdv.img.n5.N5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.WindowManager;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imagej.ImageJ;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.hcs.HCSDataSetter;
import org.embl.mobie.lib.hcs.HCSPlate;
import org.embl.mobie.lib.io.IOHelper;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.github.GitHubUtils;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.SpimDataImage;
import org.embl.mobie.lib.image.SpotAnnotationImage;
import org.embl.mobie.lib.plugins.platybrowser.GeneSearchCommand;
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
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.transformation.GridTransformation;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegmentCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.ui.UserInterface;
import org.embl.mobie.lib.ui.WindowArrangementHelper;
import org.embl.mobie.lib.view.ViewManager;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
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

import static org.embl.mobie.io.util.IOHelper.combinePath;
import static org.embl.mobie.io.util.IOHelper.getFileName;

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
	public static boolean openedFromCLI = false;
	public static ImageJ imageJ;
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";
	public static boolean initiallyShowSourceNames = false;

	private String projectLocation;
	private MoBIESettings settings;
	private Dataset dataset;
	private ViewManager viewManager;
	private Project project;
	private UserInterface userInterface;
	private String projectRoot = "";
	private String imageRoot = "";
	private String tableRoot = "";
	private HashMap< String, ImgLoader > sourceNameToImgLoader;
	private ArrayList< String > projectCommands = new ArrayList<>();

	public MoBIE( String projectLocation ) throws IOException
	{
		this( projectLocation, new MoBIESettings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		init();

		this.settings = settings;
		this.projectLocation = projectLocation;

		IJ.log("\n# MoBIE" );
		IJ.log("Opening: " + projectLocation );

		if ( settings.values.hcsProject() )
		{
			initHCSProject( projectLocation );
		}
		else
		{
			initMoBIEProject();
		}
	}

	private void initMoBIEProject() throws IOException
	{
		setS3Credentials( settings );
		setProjectImageAndTableRootLocations();
		registerProjectPlugins( projectLocation );
		project = new ProjectJsonParser().parseProject( combinePath( projectRoot, "project.json" ) );
		if ( project.getName() == null ) project.setName( getFileName( projectLocation ) );
		setDataFormats( projectLocation );
		openAndViewDataset();
	}

	public MoBIE( String projectName, String[] imagePaths, String[] segmentationPaths, String[] tablePaths, boolean combine ) throws IOException
	{
		init();

		initProject( projectName );

		if ( imagePaths != null && imagePaths[ 0 ].contains( "*" ) )
			imagePaths = IOHelper.getPaths( imagePaths[ 0 ], 999 );

		if ( segmentationPaths != null && segmentationPaths[ 0 ].contains( "*" ) )
			segmentationPaths = IOHelper.getPaths( segmentationPaths[ 0 ], 999 );

		if ( tablePaths != null && tablePaths[ 0 ].contains( "*" ) )
			tablePaths = IOHelper.getPaths( tablePaths[ 0 ], 999 );

		// load images
		if ( imagePaths != null )
		{
			for ( String path : imagePaths )
			{
				System.out.println( "Opening image: " + path );
				final AbstractSpimData< ? > spimData = IOHelper.tryOpenSpimData( path, ImageDataFormat.fromPath( path ) );
				addSpimDataImages( spimData, false, null, null );
			}
		}

		// load segmentations (with tables)
		if ( segmentationPaths != null )
		{
			for ( int segmentationIndex = 0; segmentationIndex < segmentationPaths.length; segmentationIndex++ )
			{
				final String segmentationPath = segmentationPaths[ segmentationIndex ];
				System.out.println( "Opening segmentation: " + segmentationPath );
				final ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( segmentationPath );
				final AbstractSpimData< ? > spimData = IOHelper.tryOpenSpimData( segmentationPath, imageDataFormat );

				if ( tablePaths != null && tablePaths.length > segmentationIndex )
				{
					final String tablePath = tablePaths[ segmentationIndex ];
					System.out.println( "...with segments table: " + tablePath );
					final TableDataFormat tableDataFormat = TableDataFormat.fromPath( tablePath );
					final StorageLocation tableStorageLocation = new StorageLocation();
					final File tableFile = new File( tablePath );
					tableStorageLocation.absolutePath = tableFile.getParent();
					tableStorageLocation.defaultChunk = tableFile.getName();
					addSpimDataImages( spimData, true, tableStorageLocation, tableDataFormat );
				} else
				{
					addSpimDataImages( spimData, true, null, null );
				}
			}
		}

		// if possible, combine
		// image views and segmentation views
		// into segmented image views
		// and create a grid view

		if ( combine && imagePaths != null && segmentationPaths != null )
		{
			final String[] views = dataset.views().keySet().toArray( new String[ 0 ] );
			Arrays.sort( views );

			final GridTransformation imageGridTransformation = new GridTransformation();
			imageGridTransformation.nestedSources = new ArrayList<>();
			final GridTransformation segmentationGridTransformation = new GridTransformation();
			segmentationGridTransformation.nestedSources = new ArrayList<>();

			final ArrayList< String > imageGridSources = new ArrayList<>();
			final ArrayList< String > segmentationGridSources = new ArrayList<>();
			final ArrayList< View > segmentedImageViews = new ArrayList<>();

			ImageDisplay< ? > imageDisplay = null;
			SegmentationDisplay< ? > segmentationDisplay = null;

			for ( int viewIndex = 0; viewIndex < views.length; )
			{
				final Display< ? > displayA = dataset.views().get( views[ viewIndex++ ] ).displays().get( 0 );
				final Display< ? > displayB = dataset.views().get( views[ viewIndex++ ] ).displays().get( 0 );

				if ( displayA instanceof ImageDisplay
						&& displayB instanceof SegmentationDisplay )
				{
					imageDisplay = ( ImageDisplay< ? > ) displayA;
					segmentationDisplay = ( SegmentationDisplay< ? > ) displayB;
				} else if ( displayB instanceof ImageDisplay
						&& displayA instanceof SegmentationDisplay )
				{
					imageDisplay = ( ImageDisplay< ? > ) displayB;
					segmentationDisplay = ( SegmentationDisplay< ? > ) displayA;
				}
				else
				{
					System.err.println("Could not match " + displayA.getName() + " and " + displayB.getName() );
					System.err.println("To avoid errors no combined views will be generated.");
					segmentedImageViews.clear();
					imageGridSources.clear();
					break;
				}

				final String lcsubstr = MoBIEHelper.lcsubstring( imageDisplay.getName(), segmentationDisplay.getName() );
				final String name = imageDisplay.getName() + "-" + segmentationDisplay.getName().replace( lcsubstr, "" );
				final ArrayList< Display< ? > > displays = new ArrayList<>();
				displays.add( imageDisplay );
				displays.add( segmentationDisplay );
				final View segmentedImage = new View( name, "segmented image", displays, null, true );
				segmentedImageViews.add( segmentedImage );

				//gridDisplays.add( displayB );
				imageGridSources.addAll( imageDisplay.getSources() );
				segmentationGridSources.addAll( segmentationDisplay.getSources() );
				//gridSources.addAll( displayB.getSources() );
				imageGridTransformation.nestedSources.add( imageDisplay.getSources() );
				segmentationGridTransformation.nestedSources.add( segmentationDisplay.getSources() );
			}

			if ( segmentedImageViews.size() > 0 )
			{
				for ( View segmentedImageView : segmentedImageViews )
				{
					dataset.views().put( segmentedImageView.getName(), segmentedImageView );
				}
			}

			if ( imageGridSources.size() > 1 )
			{
				final ImageDisplay< ? > imageGridDisplay = new ImageDisplay<>( "images", imageGridSources, imageDisplay.getColor(), imageDisplay.getContrastLimits() );
				final SegmentationDisplay< ? > segmentationGridDisplay = new SegmentationDisplay<>( "segmentations", segmentationGridSources );
				final View gridView = new View( "segmented images", "grid", Arrays.asList( imageGridDisplay, segmentationGridDisplay ), Arrays.asList( imageGridTransformation, segmentationGridTransformation ), true );
				dataset.views().put( gridView.getName(), gridView );
			}
			else
			{
				System.out.println( "Could not create a grid view." );
			}
		}

		// show the last added view
		final String[] viewNames = dataset.views().keySet().toArray( new String[ 0 ] );
		initUIandShowView( viewNames[ viewNames.length -1 ] );
	}

	// use this constructor from the Fiji UI
	//
	// images: convert all image data to {@code SpimData}
	// before calling this constructor.
	// {@code SpimDataOpener} in mobie-io provides methods for this.
	//
	// tables: provide the {@code StorageLocation}
	// and the {@code TableDataFormat}.
	public MoBIE( String projectName, AbstractSpimData< ? > image, AbstractSpimData< ? > segmentation, StorageLocation tableStorageLocation, TableDataFormat tableDataFormat )
	{
		init();

		initProject( projectName );

		addSpimDataImages( image, false, null, null );

		addSpimDataImages( segmentation, true, tableStorageLocation, tableDataFormat );

		initUIandShowView( null );
	}

	private void init()
	{
		DebugTools.setRootLevel( "OFF" ); // Bio-Formats logging

		if ( MoBIE.openedFromCLI )
			imageJ = new ImageJ(); // Init SciJava Services

		if ( moBIE != null )
		{
			// only allow one instance to avoid confusion
			IJ.log("Detected running MoBIE instance.");
			moBIE.close();
		}
		moBIE = this;
	}

	private void initHCSProject( String projectLocation ) throws IOException
	{
		initProject( "HCS" );
		final HCSPlate hcsPlate = new HCSPlate( projectLocation );
		IJ.log( "HCS Pattern: " + hcsPlate.getHcsPattern() );
		new HCSDataSetter().addPlateToDataset( hcsPlate, dataset );
		initUIandShowView( dataset.views().keySet().iterator().next() );
	}

	private void initUIandShowView( @Nullable String view )
	{
		initUI();

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

		adjustLogWindow();
	}

	private void adjustLogWindow()
	{
		final Window userInterfaceWindow = userInterface.getWindow();
		WindowArrangementHelper.bottomAlignWindow( userInterfaceWindow, WindowManager.getWindow( "Log" ), true, true );
	}

	/*
	Use this if there is no project.json
	 */
	private void initProject( String projectName )
	{
		settings = new MoBIESettings();
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
			dataset.addDataSource( dataSource );
			dataset.is2D( MoBIEHelper.is2D( spimData, setupIndex ) );
		}
	}

	private void addImageView( AbstractSpimData< ? > spimData, int imageIndex, String imageName )
	{
		final Displaysettings displaysettings = spimData.getSequenceDescription().getViewSetupsOrdered().get( imageIndex ).getAttribute( Displaysettings.class );

		String color = "White";
		double[] contrastLimits = null;

		if ( displaysettings != null )
		{
			// FIXME: Wrong color from Bio-Formats
			//    https://forum.image.sc/t/bio-formats-color-wrong-for-imagej-images/76021/15
			//    https://github.com/BIOP/bigdataviewer-image-loaders/issues/8
			color = "White"; // ColorHelper.getString( displaysettings.color );
			contrastLimits = new double[]{ displaysettings.min, displaysettings.max };
		}

		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, Arrays.asList( imageName ), color, contrastLimits );
		final View view = new View( imageName, "image", Arrays.asList( imageDisplay ), null, false );
		dataset.views().put( view.getName(), view );
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
		dataset.views().put( view.getName(), view );
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

	// set whether to open data from local or remote
	private void setDataFormats( String projectLocation )
	{
		// images
		//
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

		// tables
		//
		settings.addTableDataFormat( TableDataFormat.TSV );
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
		projectRoot = createPath( projectLocation, settings.values.getProjectBranch() );

		if( ! org.embl.mobie.io.util.IOHelper.exists( combinePath( projectRoot, "project.json" ) ) )
		{
			projectRoot = combinePath( projectRoot, "data" );
		}

		imageRoot = createPath(
				settings.values.getImageDataLocation() != null ? settings.values.getImageDataLocation() : projectLocation ,
				settings.values.getImageDataBranch() );

		if( ! org.embl.mobie.io.util.IOHelper.exists( combinePath( imageRoot, "project.json" ) ) )
		{
			imageRoot = combinePath( imageRoot, "data" );
		}

		tableRoot = createPath(
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
		dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );
		dataset.setName( datasetName );

		// set data source names
		for ( Map.Entry< String, DataSource > entry : dataset.sources().entrySet() )
			entry.getValue().setName( entry.getKey() );

		// log views
		System.out.println("# Available views");
		for ( String s : getViews().keySet() )
			System.out.println( s );
		System.out.println("/n");

		// build UI and show view
		initUI();
		viewManager.show( getView( viewName, dataset ) );
		adjustLogWindow();
	}

	private void initUI()
	{
		sourceNameToImgLoader = new HashMap<>();
		userInterface = new UserInterface( this );
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

	private String createPath( String rootLocation, String githubBranch, String... files )
	{
		if ( rootLocation.contains( "github.com" ) )
		{
			rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
		}

		final ArrayList< String > strings = new ArrayList<>();
		strings.add( rootLocation );
		Collections.addAll( strings, files );
		final String path = combinePath( strings.toArray( new String[0] ) );

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
		return dataset.sources().get( sourceName );
	}

	private ImageDataFormat getImageDataFormat( ImageDataSource imageSource )
	{
		final Set< ImageDataFormat > settingsFormats = settings.values.getImageDataFormats();

		if ( settingsFormats.size() == 0 )
		{
			/*
				there is no preferred image data format specified,
				thus we simply return the first (and potentially only)
				source format
			 */
			return imageSource.imageData.keySet().iterator().next();
		}

		for ( ImageDataFormat sourceFormat : imageSource.imageData.keySet() )
		{
			if ( settingsFormats.contains( sourceFormat ) )
			{
				/*
					return the first source format that
				    matches what is required by the settings
				 */
				return sourceFormat;
			}
		}

		for ( ImageDataFormat dataFormat : imageSource.imageData.keySet() )
			System.err.println("Source supports: " + dataFormat);
		for ( ImageDataFormat dataFormat : settingsFormats )
			System.err.println("Settings require: " + dataFormat);

		throw new RuntimeException( "Error identifying an image data format for: " + imageSource.getName() );
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

	public String getDatasetPath( String... files )
	{
		final String datasetRoot = combinePath( projectRoot, getDataset().getName() );
		return createPath( datasetRoot, files );
	}

	private String createPath( String root, String[] files )
	{
		String location = root;
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
			case ImageJ:
			case BioFormats:
			case BdvHDF5:
			case BdvN5:
			case BdvOmeZarr:
			case BdvOmeZarrS3: // assuming that the xml is not at storageLocation.s3Address
			case BdvN5S3: // assuming that the xml is not at storageLocation.s3Address
			case OmeZarr:
            	if ( storageLocation.absolutePath != null  )
				{
					return storageLocation.absolutePath;
				}
				else
				{
					// construct absolute from relative path
					return combinePath( imageRoot, dataset.getName(), storageLocation.relativePath );
				}
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
			final TableDataFormat tableFormat = getTableDataFormat( spotDataSource.tableData );

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
			final TableDataFormat tableFormat = getTableDataFormat( regionDataSource.tableData );
			regionDataSource.table = TableOpener.open( tableLocation, tableFormat );
			DataStore.putRawData( regionDataSource );
		}

		if ( log != null )
			IJ.log( log + dataSource.getName() );
	}

	private TableSawAnnotationTableModel< TableSawAnnotatedSegment > createTableModel( SegmentationDataSource dataSource )
	{
		final StorageLocation tableLocation = getTableLocation( dataSource.tableData );
		final TableDataFormat tableFormat = getTableDataFormat( dataSource.tableData );

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
				final String imageLocation = getImageLocation( imageDataFormat, storageLocation );
				return new SpimDataImage( imageDataFormat, imageLocation, channel, name, ThreadHelper.sharedQueue );
		}
	}

	public List< DataSource > getDataSources( Set< String > names )
	{
		return names.stream()
				.filter( name -> ( dataset.sources().containsKey( name ) ) )
				.map( s -> dataset.sources().get( s ) )
				.collect( Collectors.toList() );
	}
}
