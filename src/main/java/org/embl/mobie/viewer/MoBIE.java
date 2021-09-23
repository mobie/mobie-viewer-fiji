package org.embl.mobie.viewer;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.volatiles.SharedQueue;
import bdv.img.n5.N5ImageLoader;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.display.AnnotatedIntervalDisplay;
import org.embl.mobie.viewer.annotate.AnnotatedIntervalCreator;
import org.embl.mobie.viewer.annotate.AnnotatedIntervalTableRow;
import org.embl.mobie.viewer.playground.SourceChanger;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.embl.mobie.viewer.source.ImageDataFormat;
import org.embl.mobie.viewer.source.ImageSource;
import org.embl.mobie.viewer.source.SegmentationSource;
import org.embl.mobie.viewer.source.SpimDataOpener;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.ui.UserInterface;
import org.embl.mobie.viewer.ui.WindowArrangementHelper;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.ViewManager;
import de.embl.cba.n5.ome.zarr.loaders.N5OMEZarrImageLoader;
import de.embl.cba.tables.FileAndUrlUtils;
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
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class MoBIE
{
	public static final int N_THREADS = 8;
	public static final SharedQueue sharedQueue = new SharedQueue( N_THREADS );
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";
	public static final ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );
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
	private SourceAndConverterService sacService;
	private Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter;

	public MoBIE( String projectRoot ) throws IOException
	{
		this( projectRoot, MoBIESettings.settings() );
	}

	public MoBIE( String projectLocation, MoBIESettings settings ) throws IOException
	{
		IJ.log("MoBIE");
		this.settings = settings.projectLocation( projectLocation );
		setProjectImageAndTableRootLocations( );
		projectName = Utils.getName( projectLocation );
		PlaygroundPrefs.setSourceAndConverterUIVisibility( false );
		project = new ProjectJsonParser().parseProject( FileAndUrlUtils.combinePath( projectRoot,  "project.json" ) );
		this.settings = setImageDataFormat( projectLocation );
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();
		openDataset();
	}

	private MoBIESettings setImageDataFormat( String projectLocation )
	{
		if ( settings.values.getImageDataFormat() == null )
		{
			final List<ImageDataFormat> imageDataFormats = project.getImageDataFormats();
			if ( projectLocation.startsWith( "http" ) )
			{
				if ( imageDataFormats.contains( ImageDataFormat.OmeZarrS3 ) )
					return settings.imageDataFormat( ImageDataFormat.OmeZarrS3 );
				else if ( imageDataFormats.contains( ImageDataFormat.BdvOmeZarrS3 ) )
					return settings.imageDataFormat( ImageDataFormat.BdvOmeZarrS3 );
				else if ( imageDataFormats.contains( ImageDataFormat.BdvN5S3 ) )
					return settings.imageDataFormat( ImageDataFormat.BdvN5S3 );
				else if ( imageDataFormats.contains( ImageDataFormat.OpenOrganelleS3 ) )
					return settings.imageDataFormat( ImageDataFormat.OpenOrganelleS3 );
				else
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

	public static void mergeAnnotatedIntervalTable(List<AnnotatedIntervalTableRow> intervalTableRows, Map< String, List< String > > columns )
	{
		final HashMap< String, List< String > > referenceColumns = new HashMap<>();
		final ArrayList< String > gridIdColumn = TableColumns.getColumn( intervalTableRows, TableColumnNames.ANNOTATION_ID );
		referenceColumns.put( TableColumnNames.ANNOTATION_ID, gridIdColumn );

		// deal with the fact that the grid ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		Utils.toDoubleStrings( gridIdColumn );
		Utils.toDoubleStrings( columns.get( TableColumnNames.ANNOTATION_ID ) );

		final Map< String, List< String > > newColumns = TableColumns.createColumnsForMergingExcludingReferenceColumns( referenceColumns, columns );

		for ( Map.Entry< String, List< String > > column : newColumns.entrySet() )
		{
			TableRows.addColumn( intervalTableRows, column.getKey(), column.getValue() );
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

	public Map< String, SourceAndConverter< ? > > openSourceAndConverters( Collection< String > sources )
	{
		final long start = System.currentTimeMillis();

		Map< String, SourceAndConverter< ? > > sourceAndConverters = new ConcurrentHashMap< >();

		final ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );
		for ( String sourceName : sources )
		{
			executorService.execute( () -> {
				sourceAndConverters.put( sourceName, openSourceAndConverter( sourceName ) );
			} );
		}

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Fetched " + sourceAndConverters.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms, using " + N_THREADS + " thread(s).");

		return sourceAndConverters;
	}

	private void openDataset( String datasetName ) throws IOException
	{
		sourceNameToImgLoader = new HashMap<>();
		sourceNameToSourceAndConverter = new ConcurrentHashMap<>();
		setDatasetName( datasetName );
		dataset = new DatasetJsonParser().parseDataset( getDatasetPath( "dataset.json" ) );

		userInterface = new UserInterface( this );
		viewManager = new ViewManager( this, userInterface, dataset.is2D, dataset.timepoints );
		final View view = dataset.views.get( settings.values.getView() );
		view.setName( settings.values.getView() );
		viewManager.show( view );

		// arrange windows
		WindowArrangementHelper.setLogWindowPositionAndSize( userInterface.getWindow() );
		WindowArrangementHelper.rightAlignWindow( userInterface.getWindow(), viewManager.getSliceViewer().getWindow(), false, true );
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
		// TODO
//		sourcesDisplayManager.removeAllSourcesFromViewers();
//		sourcesDisplayManager.getBdv().close();
//		userInterface.dispose();
	}

	public synchronized ImageSource getSource(String sourceName )
	{
		return dataset.sources.get( sourceName ).get();
	}

	public SourceAndConverter< ? > openSourceAndConverter( String sourceName )
	{
		final ImageSource imageSource = getSource( sourceName );
		final String imagePath = getImagePath( imageSource );
        //new Thread( () -> IJ.log( "Opening image:\n" + imagePath ) ).start();
		IJ.log( "Opening image:\n" + imagePath );
        final ImageDataFormat imageDataFormat = settings.values.getImageDataFormat();
        SpimData spimData = new SpimDataOpener().openSpimData( imagePath, imageDataFormat, sharedQueue );
        sourceNameToImgLoader.put( sourceName, spimData.getSequenceDescription().getImgLoader() );

        final SourceAndConverterFromSpimDataCreator creator = new SourceAndConverterFromSpimDataCreator( spimData );
        SourceAndConverter<?> sourceAndConverter = creator.getSetupIdToSourceAndConverter().values().iterator().next();

        // wrap as TransformedSource such that the transformation can be modified
		sourceAndConverter = new SourceAffineTransformer( sourceAndConverter, new AffineTransform3D( ) ).getSourceOut();
        return sourceAndConverter;
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

	private List< TableRowImageSegment > loadImageSegmentsTable( String sourceName, String table )
	{
		final SegmentationSource source = ( SegmentationSource ) getSource( sourceName );

		final String defaultTablePath = getTablePath( source, table );

		final List< TableRowImageSegment > segments = Utils.createAnnotatedImageSegmentsFromTableFile( defaultTablePath, sourceName );

		return segments;
	}

	private Map< String, List< String > > loadAdditionalTable( String imageID, String tablePath )
	{
		Logger.log( "Opening table:\n" + tablePath );
		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );
		TableColumns.addLabelImageIdColumn( columns, TableColumnNames.LABEL_IMAGE_ID, imageID );
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

		Utils.waitUntilFinishedAndShutDown( executorService );

		System.out.println( "Fetched " + sources.size() + " table(s) in " + (System.currentTimeMillis() - start) + " ms, using " + N_THREADS + " thread(s).");

		return additionalTables;
	}


	private ArrayList< List< TableRowImageSegment > > loadPrimarySegmentsTables(SegmentationSourceDisplay segmentationDisplay, String table )
	{
		final ArrayList< List< TableRowImageSegment > > primaryTables = new ArrayList<>();

		// TODO: make parallel
		for ( String sourceName : segmentationDisplay.getSources() )
		{
			final List< TableRowImageSegment > primaryTable = loadImageSegmentsTable( sourceName, table );
			primaryTables.add( primaryTable );
		}

		return primaryTables;
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
		Utils.toDoubleStrings( segmentIdColumn );
		Utils.toDoubleStrings( newColumns.get( TableColumnNames.SEGMENT_LABEL_ID ) );

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

	public void appendSegmentsTables( SegmentationSourceDisplay segmentationDisplay, List< String > relativeTablePaths )
	{
		appendSegmentsTables( segmentationDisplay.getSources(), relativeTablePaths, segmentationDisplay.tableRows );
	}

	/**
	 * Primary segment tables must contain the image segment properties.
	 */
	public void loadPrimarySegmentsTables( SegmentationSourceDisplay segmentationDisplay )
	{
		segmentationDisplay.tableRows = new ArrayList<>();
		final ArrayList< List< TableRowImageSegment > > primaryTables = loadPrimarySegmentsTables( segmentationDisplay, segmentationDisplay.getTables().get( 0 ) );

		for ( List< TableRowImageSegment > primaryTable : primaryTables )
		{
			segmentationDisplay.tableRows.addAll( primaryTable );
		}
	}

	public List< AnnotatedIntervalTableRow > loadAnnotatedIntervalTables( AnnotatedIntervalDisplay annotationDisplay )
	{
		// open
		final List< Map< String, List< String > > > tables = new ArrayList<>();
		for ( String table : annotationDisplay.getTables() )
		{
			String tablePath = getTablePath( annotationDisplay.getTableDataFolder( TableDataFormat.TabDelimitedFile ), table );
			tablePath = Utils.resolveTablePath( tablePath );
			Logger.log( "Opening table:\n" + tablePath );
			tables.add( TableColumns.stringColumnsFromTableFile( tablePath ) );
		}

		// create primary AnnotatedIntervalTableRow table
		final Map< String, List< String > > referenceTable = tables.get( 0 );
		// TODO: The AnnotatedIntervalCreator does not need the sources, but just the source's real intervals
		final AnnotatedIntervalCreator annotatedIntervalCreator = new AnnotatedIntervalCreator( referenceTable, annotationDisplay.getAnnotationIdToSources(), (String sourceName ) -> this.getSourceAndConverter( sourceName )  );
		final List< AnnotatedIntervalTableRow > intervalTableRows = annotatedIntervalCreator.getAnnotatedIntervalTableRows();

		final List< Map< String, List< String > > > additionalTables = tables.subList( 1, tables.size() );

		for ( int i = 0; i < additionalTables.size(); i++ )
		{
			MoBIE.mergeAnnotatedIntervalTable( intervalTableRows, additionalTables.get( i ) );
		}

		return intervalTableRows;
	}

	public void closeSourceAndConverter( SourceAndConverter< ? > sourceAndConverter )
	{
		SourceAndConverterServices.getBdvDisplayService().removeFromAllBdvs( sourceAndConverter );
		String sourceName = sourceAndConverter.getSpimSource().getName();
		final ImgLoader imgLoader = sourceNameToImgLoader.get( sourceName );
		if ( imgLoader instanceof N5ImageLoader )
		{
			( ( N5ImageLoader ) imgLoader ).close();
		} else if ( imgLoader instanceof N5OMEZarrImageLoader ) {
            ((N5OMEZarrImageLoader) imgLoader).close();
        }

		sourceNameToImgLoader.remove( sourceName );
		SourceAndConverterServices.getSourceAndConverterService().remove( sourceAndConverter );

		// TODO - when we support more image formats e.g. OME-ZARR, we should explicitly close their imgloaders here too
	}

    public synchronized String getImagePath(ImageSource source) {
        final ImageDataFormat imageDataFormat = settings.values.getImageDataFormat();

        switch (imageDataFormat) {
            case BdvN5:
            case BdvN5S3:
            case BdvOmeZarr:
            case OmeZarr:
            case BdvOmeZarrS3:
                final String relativePath = source.imageData.get( imageDataFormat ).relativePath;
                return FileAndUrlUtils.combinePath( imageRoot, getDatasetName(), relativePath );
            case OpenOrganelleS3:
            case OmeZarrS3:
                return source.imageData.get( imageDataFormat ).s3Address;
            default:
                throw new UnsupportedOperationException( "File format not supported: " + imageDataFormat );
        }
    }

	public void addSourceAndConverters( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverters )
	{
		this.sourceNameToSourceAndConverter.putAll( sourceNameToSourceAndConverters );
	}

	public SourceAndConverter< ? > getSourceAndConverter( String sourceName )
	{
		return this.sourceNameToSourceAndConverter.get( sourceName );
	}
}
