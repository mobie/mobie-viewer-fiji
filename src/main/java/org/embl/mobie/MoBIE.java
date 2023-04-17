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
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.ImageSources;
import org.embl.mobie.lib.LabelSources;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.lib.hcs.HCSDataSetter;
import org.embl.mobie.lib.hcs.Plate;
import org.embl.mobie.lib.hcs.Site;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.SpimDataImage;
import org.embl.mobie.lib.image.SpotAnnotationImage;
import org.embl.mobie.lib.io.FileImageSource;
import org.embl.mobie.lib.io.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
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
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import org.embl.mobie.lib.source.Metadata;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.io.TableImageSource;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegmentCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.lib.transform.viewer.ImageZoomViewerTransform;
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
import java.util.LinkedHashMap;
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
	public static boolean openedFromCLI = false;
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
	private ArrayList< String > projectCommands = new ArrayList<>();

	public MoBIE( String projectLocation ) throws IOException
	{
		this( projectLocation, new MoBIESettings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		initTableSaw();

		initImageJAndMoBIE();

		this.settings = settings;
		this.projectLocation = projectLocation;

		IJ.log("\n# MoBIE" );
		IJ.log("Opening: " + projectLocation );

		openMoBIEProject();
	}

	private void initTableSaw()
	{
		// force TableSaw class loading
		// to save time during the actual loading
		// TOD: this makes no sense if we don't open a project with tables
		Table.read().usingOptions( CsvReadOptions.builderFromString( "aaa\tbbb" ).separator( '\t' ).missingValueIndicator( "na", "none", "nan" ) );
	}

	public MoBIE( String hcsDataLocation, MoBIESettings settings, double relativeWellMargin, double relativeSiteMargin ) throws IOException
	{
		initImageJAndMoBIE();

		this.settings = settings;
		this.projectLocation = hcsDataLocation;

		IJ.log("\n# MoBIE" );
		IJ.log("Opening: " + hcsDataLocation );

		openHCSDataset( relativeWellMargin, relativeSiteMargin );
	}

	public MoBIE( List< String > images, List< String > labels, List< String > labelTables, String root, GridType grid, MoBIESettings settings ) throws IOException
	{
		this.settings = settings;

		openFiles( images, labels, labelTables, root, grid );
	}

	// Open image table
	public MoBIE( String tablePath, List< String > imageColumns, List< String > labelColumns, String root, GridType grid, MoBIESettings settings ) throws IOException
	{
		this.settings = settings;

		openTable( tablePath, imageColumns, labelColumns, root, grid );
	}

	private void openTable( String tablePath, List< String > images, List< String > labels, String root, GridType gridType )
	{
		final Table table = TableOpener.openDelimitedTextFile( tablePath );

		final List< ImageSources > imageSources = new ArrayList<>();
		for ( String image : images )
		{
			final TableImageSource tableImageSource = new TableImageSource( image );
			imageSources.add( new ImageSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root,  gridType ) );
		}

		final List< LabelSources > labelSources = new ArrayList<>();
		for ( String label : labels )
		{
			final TableImageSource tableImageSource = new TableImageSource( label );
			labelSources.add( new LabelSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root,  gridType ) );
		}

		openImagesAndLabels( imageSources, labelSources );
	}

	private void openMoBIEProject() throws IOException
	{
		setS3Credentials( settings );
		setProjectImageAndTableRootLocations();
		registerProjectPlugins( projectLocation );
		project = new ProjectJsonParser().parseProject( combinePath( projectRoot, "project.json" ) );
		if ( project.getName() == null ) project.setName( getFileName( projectLocation ) );
		setDataFormats( projectLocation );
		openAndViewDataset();
	}

	// TODO: add label tables
	private void openFiles( List< String > images, List< String > labels, List< String > labelTables, String root, GridType grid )
	{
		// images
		//
		final List< ImageSources > imageSources = new ArrayList<>();
		for ( String image : images )
		{
			final FileImageSource fileImageSource = new FileImageSource( image );
			imageSources.add( new ImageSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
		}


		// labels
		//
		List< LabelSources > labelSources = new ArrayList<>();
		for ( int labelSourceIndex = 0; labelSourceIndex < labels.size(); labelSourceIndex++ )
		{
			final FileImageSource fileImageSource = new FileImageSource( labels.get( labelSourceIndex ) );

			if ( labelTables.size() > labelSourceIndex )
			{
				final String labelTable = labelTables.get( labelSourceIndex );
				labelSources.add( new LabelSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, labelTable, root, grid ) );
			}
			else
			{
				labelSources.add( new LabelSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
			}
		}

		openImagesAndLabels( imageSources, labelSources );

		// TODO consider adding back the functionality of groups for sorting the grid
		//			final List< String > groups = MoBIEHelper.getGroupNames( regex );
		//			if ( groups.size() > 0 )
		//			{
		//				final Pattern pattern = Pattern.compile( regex );
		//				final Set< String > set = new LinkedHashSet<>();
		//				for ( String path : paths )
		//				{
		//					final Matcher matcher = pattern.matcher( path );
		//					matcher.matches();
		//					set.add( matcher.group( 1 ) );
		//				}
		//
		//				final ArrayList< String > categories = new ArrayList<>( set );
		//				final int[] numSources = new int[ categories.size() ];
		//				grid.positions = new ArrayList<>();
		//				for ( String source : sources )
		//				{
		//					final Matcher matcher = pattern.matcher( source );
		//					matcher.matches();
		//					final int row = categories.indexOf( matcher.group( rowGroup ) );
		//					final int column = numSources[ row ];
		//					numSources[ row ]++;
		//					grid.positions.add( new int[]{ column, row } );
		//				}
		//			}
		//			}
		//			else
		//			{

	}

	// TODO 2D or 3D?
	private void openImagesAndLabels( List< ImageSources > images, List< LabelSources > labels )
	{
		initImageJAndMoBIE();

		initProject( "Project" );

		final ArrayList< ImageSources > allSources = new ArrayList<>();
		allSources.addAll( images );
		allSources.addAll( labels );

		// create and add data sources to the dataset
		for ( ImageSources sources : allSources )
		{
			for ( String name : sources.getSources() )
			{
				final String path = sources.getPath( name );
				ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( path );
				if ( path.endsWith( "ome.tif" ) || path.endsWith( "ome.tiff" ) )
				{
					// FIXME: for multi-color ome-tiff this seems required, however,
					//        for the HCS plate images this will not work,
					//        thus we may need different logic there than here.
					//        Maybe  ImageDataFormat.fromPath() should return BioFormats if it is
					//        OME-TIFF; this is changed now in mobie-io
					imageDataFormat = ImageDataFormat.BioFormats;
				}

				final StorageLocation storageLocation = new StorageLocation();
				storageLocation.absolutePath = path;
				storageLocation.channel = sources.getChannelIndex();
				if ( sources instanceof LabelSources )
				{
					final TableSource tableSource = ( ( LabelSources ) sources ).getLabelTable( name );
					SegmentationDataSource segmentationDataSource = SegmentationDataSource.create( name, imageDataFormat, storageLocation, tableSource );
					segmentationDataSource.preInit( false );
					dataset.addDataSource( segmentationDataSource );
				}
				else
				{
					final ImageDataSource imageDataSource = new ImageDataSource( name, imageDataFormat, storageLocation );
					imageDataSource.preInit( false );
					dataset.addDataSource( imageDataSource );
				}
			}
		}

		for ( ImageSources sources : allSources )
		{
			if ( sources.getGridType().equals( GridType.Stitched ) )
			{
				// init table for the RegionDisplay
				final StorageLocation storageLocation = new StorageLocation();
				storageLocation.data = sources.getRegionTable();
				final RegionDataSource regionDataSource = new RegionDataSource( sources.getName() );
				regionDataSource.addTable( TableDataFormat.Table, storageLocation );
				DataStore.putRawData( regionDataSource );

				// init RegionDisplay
				final RegionDisplay< AnnotatedRegion > regionDisplay = new RegionDisplay<>( sources.getName() + " regions" );
				regionDisplay.sources = new LinkedHashMap<>();
				regionDisplay.tableSource = regionDataSource.getName();
				regionDisplay.showAsBoundaries( true );
				regionDisplay.setBoundaryThickness( 0.05 );
				regionDisplay.boundaryThicknessIsRelative( true );
				regionDisplay.setOpacity( 1.0 );
				final int numTimePoints = sources.getMetadata().numTimePoints;
				for ( int t = 0; t < numTimePoints; t++ )
					regionDisplay.timepoints().add( t );

				final List< String > sourceNames = sources.getSources();
				final int numRegions = sourceNames.size();
				for ( int regionIndex = 0; regionIndex < numRegions; regionIndex++ )
				{
					regionDisplay.sources.put( sourceNames.get( regionIndex ), Collections.singletonList( sourceNames.get( regionIndex ) ) );
				}

				// create grid transformation
				final MergedGridTransformation grid = new MergedGridTransformation( sources.getName() );
				grid.sources = sources.getSources();
				grid.metadataSource = sources.getMetadataSource();

				// create displays
				//
				final ArrayList< Display< ? > > displays = new ArrayList<>();

				if ( sources instanceof LabelSources )
				{
					// SegmentationDisplay
					displays.add( new SegmentationDisplay<>( grid.getName(), Collections.singletonList( grid.getName() ) ) );
				}
				else
				{
					// ImageDisplay
					final Metadata metadata = sources.getMetadata();
					displays.add( new ImageDisplay<>( grid.getName(), Collections.singletonList( grid.getName() ), metadata.color, metadata.contrastLimits ) );
				}

				displays.add( regionDisplay );

				// create grid view
				//
				final ImageZoomViewerTransform viewerTransform = new ImageZoomViewerTransform( grid.getSources().get( 0 ), 0 );
				final View gridView = new View( sources.getName(), "grids", displays, Arrays.asList( grid ), viewerTransform, false );
				//gridView.overlayNames( true ); // Timepoint bug:
				dataset.views().put( gridView.getName(), gridView );
			}
			else
			{
				//					for ( int gridPosition = 0; gridPosition < numRegions; gridPosition++ )
//					{
//						try
//						{
//							final String sourceName = sources.get( gridPosition );
//							grid.nestedSources.get( gridPosition ).add( sourceName );
//							regionDisplay.sources.get( "grid_" + gridPosition ).add( sourceName );
//						}
//						catch ( Exception e )
//						{
//							e.printStackTrace();
//						}
//					}
//				}

				throw new UnsupportedOperationException( "Grid type currently not supported.");
			}
		}

		initUIandShowView( dataset.views().keySet().iterator().next() );
	}

	// use this constructor from the Fiji UI
	//
	// images: convert all image data to {@code SpimData}
	// before calling this constructor.
	// {@code SpimDataOpener} in mobie-io provides methods for this.
	//
	// tables: provide the {@code StorageLocation}
	// and the {@code TableDataFormat}.
	public MoBIE( String projectName, AbstractSpimData< ? > image, @Nullable AbstractSpimData< ? > segmentation, @Nullable StorageLocation tableStorageLocation, @Nullable TableDataFormat tableDataFormat )
	{
		initImageJAndMoBIE();

		initProject( projectName );

		addSpimDataImages( image, false, null, null );

		if ( segmentation != null )
			addSpimDataImages( segmentation, true, tableStorageLocation, tableDataFormat );

		initUIandShowView( null );
	}

	private void initImageJAndMoBIE()
	{
		DebugTools.setRootLevel( "OFF" ); // Disable Bio-Formats logging

		if ( MoBIE.openedFromCLI )
		{
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
		new HCSDataSetter().addPlateToDataset( plate, dataset, wellMargin, siteMargin );
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

	/*
	Use this if there is no project.json
	 */
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
			//System.out.println( imageName + ": contrast limits = " + Arrays.toString( contrastLimits ) );
		}

		final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( imageName, Arrays.asList( imageName ), color, contrastLimits );
		final View view = new View( imageName, "images", Arrays.asList( imageDisplay ), null, false );
		dataset.views().put( view.getName(), view );
	}

	private String getImageName( String imagePath, int numImages, int imageIndex )
	{
		String imageName = FilenameUtils.removeExtension( new File( imagePath ).getName() );
		if ( numImages > 1 )
			imageName += "_ch" + imageIndex;
		return imageName;
	}

	private void addSegmentationView( String imageName, AbstractSpimData< ? > spimData, int setupId  )
	{
		final SegmentationDisplay< ? > display = new SegmentationDisplay<>( imageName, Arrays.asList( imageName ) );

		final BasicViewSetup viewSetup = spimData.getSequenceDescription().getViewSetupsOrdered().get( setupId );
		final double pixelWidth = viewSetup.getVoxelSize().dimension( 0 );
		display.setResolution3dView( new Double[]{ pixelWidth, pixelWidth, pixelWidth } );

		final View view = new View( imageName, "segmentations", Arrays.asList( display ), null, false );
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

	public String absolutePath( String... files )
	{
		final String datasetRoot = combinePath( projectRoot, getDataset().getName() );
		String location = datasetRoot;
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
			case Tiff:
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
			final Image< ? > image = initImage( imageDataFormat, storageLocation, imageSource.getName() );

			if ( dataSource.preInit() )
			{
				// force initialization here to save time later
				// (i.e. help smooth rendering in BDV)
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
					// label image representing annotated segments
					TableSawAnnotationTableModel< TableSawAnnotatedSegment > tableModel = createTableModel( segmentationDataSource );
					final DefaultAnnData< TableSawAnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
					final DefaultAnnotationAdapter< TableSawAnnotatedSegment > annotationAdapter = new DefaultAnnotationAdapter( annData );
					final AnnotatedLabelImage< TableSawAnnotatedSegment > annotatedLabelImage = new DefaultAnnotatedLabelImage( image, annData, annotationAdapter );
					DataStore.putImage( annotatedLabelImage );
				}
				else
				{
					// label image representing segments without annotation table

					final LazyAnnotatedSegmentTableModel tableModel = new LazyAnnotatedSegmentTableModel( image.getName() );
					final DefaultAnnData< AnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
					final LazyAnnotatedSegmentAdapter segmentAdapter = new LazyAnnotatedSegmentAdapter( image.getName(), tableModel );
					final DefaultAnnotatedLabelImage< ? > annotatedLabelImage = new DefaultAnnotatedLabelImage( image, annData, segmentAdapter );
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

		Table table = null;
		SegmentColumnNames segmentColumnNames = null;
		if ( dataSource.preInit() )
			table = TableOpener.open( tableLocation, tableFormat );
		if ( table != null )
			segmentColumnNames = TableDataFormat.getSegmentColumnNames( table.columnNames() );

		final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( segmentColumnNames, table );

		final TableSawAnnotationTableModel tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableLocation, tableFormat, table );

		return tableModel;
	}

	private SpimDataImage< ? > initImage( ImageDataFormat imageDataFormat, StorageLocation storageLocation, String name )
	{
		if ( imageDataFormat.equals( ImageDataFormat.SpimData ) )
		{
			return new SpimDataImage<>( ( AbstractSpimData ) storageLocation.data, storageLocation.channel, name, settings.values.getRemoveSpatialCalibration() );
		}

		if ( storageLocation instanceof Site )
		{
			return new SpimDataImage( ( Site ) storageLocation, name );
		}

		// TODO improve caching: https://github.com/mobie/mobie-viewer-fiji/issues/857
		final String imagePath = getImageLocation( imageDataFormat, storageLocation );
		final SpimDataImage spimDataImage = new SpimDataImage( imageDataFormat, imagePath, storageLocation.channel, name, ThreadHelper.sharedQueue, settings.values.getRemoveSpatialCalibration() );
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
