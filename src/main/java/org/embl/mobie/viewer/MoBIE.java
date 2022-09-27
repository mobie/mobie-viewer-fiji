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
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.github.GitHubUtils;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.image.SpotLabelImage;
import org.embl.mobie.viewer.plugins.platybrowser.GeneSearchCommand;
import org.embl.mobie.viewer.serialize.DataSource;
import org.embl.mobie.viewer.serialize.Dataset;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ImageDataSource;
import org.embl.mobie.viewer.serialize.Project;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.embl.mobie.viewer.serialize.SegmentationDataSource;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.SpimDataImage;
import org.embl.mobie.viewer.serialize.SpotDataSource;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.DefaultAnnData;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.viewer.table.saw.TableSawAnnotationTableModel;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedSegmentCreator;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewManager;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.IOException;
import java.util.ArrayList;
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
			return "Initialising (" + index.get() + "/" + numTotal + "): ";
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
		setDatasetName( datasetName );
		dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );
		for ( Map.Entry< String, DataSource > entry : dataset.sources.entrySet() )
			entry.getValue().setName( entry.getKey() );
		userInterface = new UserInterface( this );
		viewManager = new ViewManager( this, userInterface, dataset.is2D );
		for ( String s : getViews().keySet() )
			System.out.println( s );

		final View view = getSelectedView();
		view.setName( settings.values.getView() );
		IJ.log( "Opening view: " + view.getName() );
		final long startTime = System.currentTimeMillis();
		viewManager.show( view );
		IJ.log("Opened view: " + view.getName() + ", in " + (System.currentTimeMillis() - startTime) + " ms." );
	}

	private View getSelectedView() throws IOException
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

	private ImageDataFormat getAppropriateImageDataFormat( ImageDataSource imageSource )
	{
		for ( ImageDataFormat sourceDataFormat : imageSource.imageData.keySet() )
		{
			if ( settings.values.getImageDataFormats().contains( sourceDataFormat ) )
			{
				return sourceDataFormat;
			}
		}

		System.err.println("Error opening: " + imageSource.getName() );
		for ( ImageDataFormat dataFormat : imageSource.imageData.keySet() )
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
				return ( SpimData ) new SpimDataOpener().openSpimData( imagePath, imageDataFormat, ThreadHelper.sharedQueue );
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

    private String getRelativeTablePath( Map< TableDataFormat, StorageLocation > tableData )
    {
        return tableData.get( TableDataFormat.TabDelimitedFile ).relativePath;
    }

    public String getTableDirectory( Map< TableDataFormat, StorageLocation > tableData )
    {
		final String relativeTablePath = getRelativeTablePath( tableData );
		return getTableDirectory( relativeTablePath );
    }

    public String getTableDirectory( String relativeTableLocation )
    {
        return IOHelper.combinePath( tableRoot, datasetName, relativeTableLocation );
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

    public synchronized String getImagePath( ImageDataSource source, ImageDataFormat imageDataFormat) {

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

	public ArrayList< String > getProjectCommands()
	{
		return projectCommands;
	}

	public void initDataSources( List< DataSource > dataSources )
	{
		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		AtomicInteger sourceIndex = new AtomicInteger(0);
		final int numImages = dataSources.size();
		AtomicInteger sourceLoggingModulo = new AtomicInteger(1);
		AtomicLong lastLogMillis = new AtomicLong( System.currentTimeMillis() );
		final long startTime = System.currentTimeMillis();
		for ( DataSource dataSource : dataSources )
		{
			if ( ImageStore.contains( dataSource.getName() ) )
				continue;

			futures.add(
					ThreadHelper.ioExecutorService.submit( () -> {
								String log = getLog( sourceIndex, numImages, sourceLoggingModulo, lastLogMillis );
								initDataSource( dataSource, log );
							}
					) );
		}

		ThreadHelper.waitUntilFinished( futures );
		IJ.log( "Initialised " + dataSources.size() + " image(s) in " + (System.currentTimeMillis() - startTime) + " ms, using up to " + ThreadHelper.getNumIoThreads() + " thread(s).");
	}

	private void initDataSource( DataSource dataSource, String log )
	{
		if ( dataSource instanceof ImageDataSource )
		{
			ImageDataSource imageSource = ( ImageDataSource ) dataSource;
			ImageDataFormat imageDataFormat = getAppropriateImageDataFormat( imageSource );
			final String imagePath = getImagePath( imageSource, imageDataFormat );
			final SpimDataImage< ? > image = new SpimDataImage( imageDataFormat, imagePath, 0, dataSource.getName(), ThreadHelper.sharedQueue );

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
					final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( image.getName() );
					final TableSawAnnotationTableModel tableModel = new TableSawAnnotationTableModel( annotationCreator, getTableDirectory( segmentationDataSource.tableData ), "default.tsv"  );
					final DefaultAnnData< TableSawAnnotatedSegment > segmentsAnnData = new DefaultAnnData<>( tableModel );
					final AnnotatedLabelImage annotatedLabelImage = new AnnotatedLabelImage( image, segmentsAnnData );

					// label image representing annotated segments
					ImageStore.putRawImage( annotatedLabelImage );
				} else
				{
					// label image representing segments
					// without annotation
					ImageStore.putRawImage( image );
				}
			} else
			{
				// intensity image
				ImageStore.putRawImage( image );
			}
		}

		if ( dataSource instanceof SpotDataSource )
		{
			final SpotDataSource spotDataSource = ( SpotDataSource ) dataSource;
			final TableSawAnnotationCreator< TableSawAnnotatedSpot > annotationCreator = new TableSawAnnotatedSpotCreator();
			final TableSawAnnotationTableModel< AnnotatedSpot > tableModel = new TableSawAnnotationTableModel( annotationCreator, moBIE.getTableDirectory( spotDataSource.tableData.dataStore ), spotDataSource.tableData.defaultTable );
			final Set< AnnotatedSpot > annotatedSpots = tableModel.annotations();
			final Image< UnsignedIntType > labelImage = new SpotLabelImage<>( spotDataSource.getName(), annotatedSpots, 1.0 );
			final DefaultAnnData< AnnotatedSpot > spotAnnData = new DefaultAnnData<>( tableModel );
			final AnnotatedLabelImage spotsImage = new AnnotatedLabelImage( labelImage, spotAnnData );
			ImageStore.putImage( spotsImage );
		}

		if ( log != null )
			IJ.log( log + dataSource.getName() );
	}

	public Set< String > getTablePaths( Map< TableDataFormat, StorageLocation > tableData )
	{
		final String tableDirectory = getTableDirectory( tableData );
		String[] fileNames = IOHelper.getFileNames( tableDirectory );
		//"https://raw.githubusercontent.com/mobie/clem-example-project/main/data/hela/tables/em-detail"
		final Set< String > columnPaths = new HashSet<>();
		for ( String fileName : fileNames )
			columnPaths.add( IOHelper.combinePath( tableDirectory, fileName ) );
		return columnPaths;
	}

	public List< DataSource > getSources( Set< String > imageNames )
	{
		final List< DataSource > dataSources = imageNames.stream().filter( name -> ( getDataset().sources.containsKey( name ) ) ).map( s -> getDataset().sources.get( s ) ).collect( Collectors.toList() );
		return dataSources;
	}
}
