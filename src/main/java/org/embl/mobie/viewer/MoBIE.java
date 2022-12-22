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
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.LUT;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imglib2.Dimensions;
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
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.DefaultAnnData;
import org.embl.mobie.viewer.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.viewer.table.SegmentColumnNames;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MoBIE
{
	static {
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
	private ArrayList< String > projectCommands = new ArrayList<>();;
	public static int minLogTimeMillis = 100;
	public static boolean initiallyShowSourceNames = false;

	public MoBIE( String projectLocation ) throws IOException
	{
		this( projectLocation, MoBIESettings.settings() );
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
			project.setName( MoBIEHelper.getFileName( projectLocation ) );
		setImageDataFormats( projectLocation );

		openAndViewDataset();
	}

	public MoBIE( String projectName, String[] imagePaths, String[] segmentationPaths ) throws SpimDataException
	{
		// init settings, project and dataset
		settings = new MoBIESettings();
		project = new Project( projectName, true );
		currentDatasetName = project.getName();
		project.datasets().add( currentDatasetName );
		project.setDefaultDataset( currentDatasetName );
		// FIXME where is the link of a dataset to its name?
		dataset = new Dataset();
		dataset.is2D = true; // changed further down

		// init images: data and corresponding display
		for ( String imagePath : imagePaths )
			addImages( imagePath );

		// init images: data and corresponding display
		// TODO add tablePaths?!
		for ( String segmentationPath : segmentationPaths )
			addSegmentations( segmentationPath );

		// show one view on the dataset
		final String viewName = dataset.views.keySet().iterator().next();
		initUI();
		viewManager.show( getView( viewName, dataset ) );

		//viewManager.show( segmentationView );

////		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( image, displaysettings );
//		final int channel = 0;  // TODO: For loop
//
//
//
//
//		// intensity display
//		// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/923
//		intensityImp.setC( channel );
//		final ImageProcessor processor = intensityImp.getProcessor();
//		final LUT lut = processor.getLut();
//		final String color = ColorHelper.getString( lut );
//		final double[] contrastLimits = { processor.getMin(), processor.getMax() };
//
//		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, Arrays.asList( imageName ), color, contrastLimits, false, null );
//		final View intensityView = new View( imageName, "intensity", Arrays.asList( imageDisplay ), null, false );
//		dataset.views.put( intensityView.getName(), intensityView );
//
//		// init segmentations
//		final String labelImageName = labelImp.getTitle();
//		final StorageLocation labelStorageLocation = new StorageLocation();
//		labelStorageLocation.data = labelImp;
//		final SegmentationDataSource segmentationDataSource = new SegmentationDataSource( labelImageName, ImageDataFormat.ImagePlus, labelStorageLocation );
//		dataset.sources.put( segmentationDataSource.getName(), segmentationDataSource );
//
//		// segmentation display
//		final SegmentationDisplay< AnnotatedSegment > segmentationDisplay = new SegmentationDisplay<>( labelImageName, Arrays.asList( segmentationDataSource.getName() ) );
//		final View segmentationView = new View( labelImageName, "segmentation", Arrays.asList( segmentationDisplay ), null, false );
//		dataset.views.put( segmentationView.getName(), segmentationView );


	}

	private void addImages( String imagePath ) throws SpimDataException
	{
		final ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( imagePath );
		settings.addImageDataFormat( imageDataFormat );
		final AbstractSpimData< ? > spimData = new SpimDataOpener().openSpimData( imagePath, imageDataFormat );

		// spimData can contain multiple images
		// (bio-formats series and/or channels)
		// (moBIE images are single channel)
		final int numImages = spimData.getSequenceDescription().getViewSetupsOrdered().size();

		for ( int imageIndex = 0; imageIndex < numImages; imageIndex++ )
		{
			// configure {@code DataSource}
			final StorageLocation storageLocation = configureImageStorageLocation( imagePath, imageIndex, ImageDataFormat.fromPath( imagePath ) );
			String imageName = getImageName( imagePath, numImages, imageIndex );

			final ImageDataSource dataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
			dataSource.preInit( true );

			// amend {@code Dataset} with the new {@code DataSource}
			addSourceToDataset( spimData, imageIndex, dataSource );

			// configure corresponding {@code Display}
			final Displaysettings displaysettings = spimData.getSequenceDescription().getViewSetupsOrdered().get( imageIndex ).getAttribute( Displaysettings.class );
			if ( displaysettings != null )
			{
				final String color = ColorHelper.getString( displaysettings.color );
				final double[] contrastLimits = { displaysettings.min, displaysettings.max };

				final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, Arrays.asList( imageName ), color, contrastLimits, false, null );
				final View view = new View( imageName, "image", Arrays.asList( imageDisplay ), null, false );
				dataset.views.put( view.getName(), view );
			}
			else
			{
				// TODO: auto-display
				throw new UnsupportedOperationException("Please contact @tischi to fix this :)");
			}

		}
	}

	private String getImageName( String imagePath, int numImages, int imageIndex )
	{
		String imageName = new File( imagePath ).getName();
		if ( numImages > 1 )
			imageName += "_" + imageIndex;
		return imageName;
	}

	private void addSegmentations( String imagePath ) throws SpimDataException
	{
		final ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( imagePath );
		settings.addImageDataFormat( imageDataFormat );
		final AbstractSpimData< ? > spimData = new SpimDataOpener().openSpimData( imagePath, imageDataFormat );

		// spimData can contain multiple images
		// (bio-formats series and/or channels)
		// (moBIE images are single channel)
		final int numImages = spimData.getSequenceDescription().getViewSetupsOrdered().size();

		for ( int imageIndex = 0; imageIndex < numImages; imageIndex++ )
		{
			// configure {@code SegmentationDataSource}
			final StorageLocation storageLocation = configureImageStorageLocation( imagePath, imageIndex, ImageDataFormat.fromPath( imagePath ) );
			String imageName = getImageName( imagePath, numImages, imageIndex );
			final SegmentationDataSource dataSource = new SegmentationDataSource( imageName, imageDataFormat, storageLocation );
			dataSource.preInit( true );

			// amend Dataset with the new {@code DataSource}
			addSourceToDataset( spimData, imageIndex, dataSource );

			// configure corresponding {@code Display}
			final SegmentationDisplay< ? > display = new SegmentationDisplay<>( imageName, Arrays.asList( imageName ) );
			final View view = new View( imageName, "segmentation", Arrays.asList( display ), null, false );
			dataset.views.put( view.getName(), view );
		}
	}

	private void addSourceToDataset( AbstractSpimData< ? > spimData, int imageIndex, ImageDataSource segmentationDataSource )
	{
		dataset.sources.put( segmentationDataSource.getName(), segmentationDataSource );
		if ( dataset.is2D )
		{
			final Dimensions dimensions = spimData.getSequenceDescription().getViewSetupsOrdered().get( imageIndex ).getSize();
			if ( dimensions.dimension( 2 ) > 1 )
				dataset.is2D = false;
		}
	}

	private StorageLocation configureImageStorageLocation( String imagePath, int channel, ImageDataFormat imageDataFormat )
	{
		final StorageLocation imageStorageLocation = new StorageLocation();
		imageStorageLocation.channel = channel;

		if ( imageDataFormat.isRemote() )
		{
			imageStorageLocation.s3Address = imagePath;
			return imageStorageLocation;
		}

		if ( project.isFromCLI() )
		{
			imageStorageLocation.absolutePath = imagePath;
			return imageStorageLocation;
		}

		// MoBIE project on disk or GitHub
		imageStorageLocation.relativePath = imagePath;
		return imageStorageLocation;
	}

	public MoBIE( String projectName, ImagePlus intensityImp, ImagePlus labelImp )
	{
		settings = new MoBIESettings();
		settings.addImageDataFormat( ImageDataFormat.ImagePlus );

		// project and dataset
		project = new Project( projectName, true );
		currentDatasetName = project.getName();
		project.datasets().add( currentDatasetName );
		project.setDefaultDataset( currentDatasetName );
		// FIXME where is the link of a dataset to its name?
		dataset = new Dataset();
		dataset.is2D = intensityImp.getNSlices() == 1;

		// intensity image
		final int channel = 0;  // TODO: For loop

		final String intensityImageName = intensityImp.getTitle();
		final StorageLocation intensityStorageLocation = new StorageLocation();
		intensityStorageLocation.data = intensityImp;
		intensityStorageLocation.channel = channel;
		final ImageDataSource imageDataSource = new ImageDataSource( intensityImageName, ImageDataFormat.ImagePlus, intensityStorageLocation );
		dataset.sources.put( imageDataSource.getName(), imageDataSource );

		// intensity display
		intensityImp.setC( channel );
		final ImageProcessor processor = intensityImp.getProcessor();
		final LUT lut = processor.getLut();
		final String color = ColorHelper.getString( lut );
		final double[] contrastLimits = { processor.getMin(), processor.getMax() };

		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( intensityImageName, Arrays.asList( intensityImageName ), color, contrastLimits, false, null );
		final View intensityView = new View( intensityImageName, "intensity", Arrays.asList( imageDisplay ), null, false );
		dataset.views.put( intensityView.getName(), intensityView );

		// label image
		final String labelImageName = labelImp.getTitle();
		final StorageLocation labelStorageLocation = new StorageLocation();
		labelStorageLocation.data = labelImp;
		final SegmentationDataSource segmentationDataSource = new SegmentationDataSource( labelImageName, ImageDataFormat.ImagePlus, labelStorageLocation );
		dataset.sources.put( segmentationDataSource.getName(), segmentationDataSource );

		// segmentation display
		final SegmentationDisplay< AnnotatedSegment > segmentationDisplay = new SegmentationDisplay<>( labelImageName, Arrays.asList( segmentationDataSource.getName() ) );
		final View segmentationView = new View( labelImageName, "segmentation", Arrays.asList( segmentationDisplay ), null, false );
		dataset.views.put( segmentationView.getName(), segmentationView );

		// open UI and show view
		initUI();
		viewManager.show( getView( intensityImageName, dataset ) );

		// intensity image
//		final AbstractSpimData< ? > intensitySpimData = ImagePlusToSpimData.getSpimData( intensityImp );
//		final SpimDataImage< ? > image = new SpimDataImage<>( intensitySpimData, 0, intensityImp.getTitle(), null );
//		final Displaysettings displaysettings = intensitySpimData.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getAttribute( Displaysettings.class );
//		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( image, displaysettings );
//		viewManager.showImageDisplay( imageDisplay );

//		new MoBIEViewerTransformAdjuster( viewManager.getSliceViewer().getBdvHandle(), imageDisplay ).applyMultiSourceTransform();

		// segmentation image
//		final AbstractSpimData< ? > labelSpimData = ImagePlusToSpimData.getSpimData( labelImp );
//		final SpimDataImage< ? > labelImage = new SpimDataImage<>( labelSpimData, 0, labelImp.getTitle(), null );
//
//		// lazy segment table
//		final LazyAnnotatedSegmentTableModel tableModel = new LazyAnnotatedSegmentTableModel( labelImage.getName() );
//		final DefaultAnnData< AnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
//		final LazyAnnotatedSegmentAdapter segmentAdapter = new LazyAnnotatedSegmentAdapter( image.getName(), tableModel );
//		final DefaultAnnotatedLabelImage< AnnotatedSegment > annotatedLabelImage = new DefaultAnnotatedLabelImage( labelImage, annData, segmentAdapter );
//		final SegmentationDisplay< AnnotatedSegment > segmentationDisplay = new SegmentationDisplay<>( annotatedLabelImage );




//		final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( table );
//		tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, getTableStore( segmentationDataSource.tableData ), TableDataFormat.DEFAULT_TSV  );
//		final ResultsTableAnnotationTableModel tableModel = new ResultsTableAnnotationTableModel( resultsTable );
//		final DefaultAnnData< TableSawAnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
//		final DefaultAnnotationAdapter< TableSawAnnotatedSegment > annotationAdapter = new DefaultAnnotationAdapter( annData );
//		final AnnotatedLabelImage< TableSawAnnotatedSegment > annotatedLabelImage = new DefaultAnnotatedLabelImage( labelImage, annData, annotationAdapter );
//		final SegmentationDisplay< ? > segmentationDisplay = new SegmentationDisplay( labelImage );

//		datasetName = intensityImp.getTitle();
//		IJ.log("Opening: " + datasetName );
//		new ImageDisplay<>()
//		setDatasetName( datasetName );
//		//dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );
//		//userInterface = new UserInterface( this );
//		viewManager = new ViewManager( this, userInterface, dataset.is2D );
//		for ( String s : getViews().keySet() )
//			System.out.println( s );
//		final View view = getSelectedView( viewName );
//		view.setName( viewName );
//		new View(  );
//

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

	public TableDataFormat getTableDataFormat( Map< TableDataFormat, StorageLocation > tableData )
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

    private String getRelativeTablePath( Map< TableDataFormat, StorageLocation > tableData )
    {
		return tableData.get( getTableDataFormat( tableData ) ).relativePath;
    }

    public String getTableStore( Map< TableDataFormat, StorageLocation > tableData )
    {
		if ( project.isFromCLI() )
			return tableData.get( getTableDataFormat( tableData ) ).absolutePath;

		final String relativeTablePath = getRelativeTablePath( tableData );
		return getTableStore( relativeTablePath );
    }

    public String getTableStore( String relativeTableLocation )
    {
        return IOHelper.combinePath( tableRoot, currentDatasetName, relativeTableLocation );
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

    public synchronized String getImagePath( ImageDataFormat imageDataFormat, StorageLocation storageLocation )
	{
		switch (imageDataFormat) {
			case BioFormats:
			case BdvHDF5:
			case BdvN5:
			case BdvOmeZarr:
			case BdvOmeZarrS3: // assuming that the xml is not at storageLocation.s3Address
			case BdvN5S3: // assuming that the xml is not at storageLocation.s3Address
			case OmeZarr:
            	if ( project.isFromCLI() )
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
						} catch ( SpimDataException e )
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
			ImageDataSource imageSource = ( ImageDataSource ) dataSource;
			ImageDataFormat imageDataFormat = getImageDataFormat( imageSource );
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
			final String defaultTablePath = getDefaultTableLocation( spotDataSource.tableData );
			final TableDataFormat tableDataFormat = getTableDataFormat( spotDataSource.tableData );

			Table table = TableOpener.openTable( defaultTablePath, tableDataFormat, -1 ); // 1000

			final TableSawAnnotationCreator< TableSawAnnotatedSpot > annotationCreator = new TableSawAnnotatedSpotCreator( table );

			final TableSawAnnotationTableModel< AnnotatedSpot > tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, moBIE.getTableStore( spotDataSource.tableData ), defaultTablePath, tableDataFormat, table );

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

			regionDataSource.table = TableOpener.openTable( getDefaultTableLocation( regionDataSource.tableData ), getTableDataFormat( regionDataSource.tableData ), -1 );;

			DataStore.putRawData( regionDataSource );
		}

		if ( log != null )
			IJ.log( log + dataSource.getName() );
	}

	private TableSawAnnotationTableModel< TableSawAnnotatedSegment > createTableModel( SegmentationDataSource dataSource )
	{
		final String tableStore = getTableStore( dataSource.tableData );
		final String defaultTablePath = getDefaultTableLocation( dataSource.tableData );
		final TableDataFormat tableDataFormat = getTableDataFormat( dataSource.tableData );
		final SegmentColumnNames segmentColumnNames = tableDataFormat.getSegmentColumnNames();

		Table table = dataSource.preInit() ?
				TableOpener.openTable( defaultTablePath, tableDataFormat, -1 ) : null;

		final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( segmentColumnNames, table );

		final TableSawAnnotationTableModel tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableStore, defaultTablePath, tableDataFormat, table );

		return tableModel;

	}

	public StorageLocation getDefaultTableLocation( Map< TableDataFormat, StorageLocation > tableData )
	{
		final StorageLocation storageLocation = new StorageLocation();

		if ( project.isFromCLI() )
		{
			storageLocation.absolutePath = getTableStore( tableData );
			return storageLocation;
		}

		storageLocation.absolutePath = IOHelper.combinePath( getTableStore( tableData ), TableDataFormat.DEFAULT_TSV );
		return storageLocation;
	}

	private SpimDataImage< ? > initImage( ImageDataFormat imageDataFormat, Integer channel, StorageLocation storageLocation, String name ) throws SpimDataException
	{
		// FIXME: replace with imageDataFormat.isInMemory()
		if ( imageDataFormat.equals( ImageDataFormat.ImagePlus ) )
		{
			final AbstractSpimData spimData = new SpimDataOpener().asSpimData( storageLocation.data, imageDataFormat );
			return new SpimDataImage<>( spimData, channel, name );
		}
		else
		{
			final String imagePath = getImagePath( imageDataFormat, storageLocation );
			// TODO  Caching? https://github.com/mobie/mobie-viewer-fiji/issues/857
			return new SpimDataImage( imageDataFormat, imagePath, channel, name, ThreadHelper.sharedQueue );
		}
	}

	public Set< String > getTablePaths( Map< TableDataFormat, StorageLocation > tableData )
	{
		final String tableDirectory = getTableStore( tableData );
		String[] fileNames = IOHelper.getFileNames( tableDirectory );
		//"https://raw.githubusercontent.com/mobie/clem-example-project/main/data/hela/tables/em-detail"
		final Set< String > columnPaths = new HashSet<>();
		for ( String fileName : fileNames )
			columnPaths.add( IOHelper.combinePath( tableDirectory, fileName ) );
		return columnPaths;
	}

	public List< DataSource > getDataSources( Set< String > names )
	{
		return names.stream()
				.filter( name -> ( dataset.sources.containsKey( name ) ) )
				.map( s -> dataset.sources.get( s ) )
				.collect( Collectors.toList() );
	}
}
