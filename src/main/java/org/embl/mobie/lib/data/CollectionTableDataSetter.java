package org.embl.mobie.lib.data;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.util.Constants;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.*;
import org.embl.mobie.lib.serialize.display.*;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.GridTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CollectionTableDataSetter
{
    public static final String NO_GRID_POSITION = "no grid position";
    private final Table table;
    private final String rootPath;

    private final Map< String, String > viewToGroup = new LinkedHashMap<>();
    private final Map< String, Set< String > > viewToGrids = new LinkedHashMap<>();
    private final Map< String, List< String > > gridToSources = new LinkedHashMap<>();
    private final Map< String, List< int[] > > gridToPositions = new LinkedHashMap<>();
    private final Map< String, Map< String, List< String > > > gridToPositionsToSources = new LinkedHashMap<>();
    private final Map< String, Set< Display< ? > > > viewToDisplays = new LinkedHashMap<>();
    private final Map< String, Boolean > viewToExclusive = new LinkedHashMap<>();
    private final Map< String, List< Transformation > > viewToTransformations = new LinkedHashMap<>();
    private final Map< String, List< Integer > > gridToRowIndices = new LinkedHashMap<>();

    public CollectionTableDataSetter( Table table, String rootPath )
    {
        this.table = table;
        this.rootPath = rootPath;
    }

    public void addToDataset( Dataset dataset )
    {
        if ( ! table.containsColumn( CollectionTableConstants.URI ) )
            throw new RuntimeException( "Column \"" + CollectionTableConstants.URI + "\" must be present in the collection table." );

        Map< String, Display< ? > > displays = new HashMap< String, Display< ? >>();

        AtomicInteger sourceIndex = new AtomicInteger();
        int numRows = table.rowCount();
        table.forEach( row ->
        {
            IJ.log("Adding source " + ( sourceIndex.incrementAndGet() ) + "/" + numRows + "...");
            
            String sourceName = getName( row );
            String displayName = getDisplayName( row );

            addSource( dataset, row, sourceName, displays, displayName );

            String viewName = getViewName( row );
            String gridName = getGridId( row );
            if ( gridName != null )
            {
                gridToRowIndices.computeIfAbsent( gridName, k -> new ArrayList<>() ).add( row.getRowNumber() );
                viewToGrids.computeIfAbsent( viewName, k -> new HashSet<>() ).add( gridName );
                String gridPosition = getGridPosition( row );
                gridToPositionsToSources
                            .computeIfAbsent( gridName, k -> new LinkedHashMap<>() )
                            .computeIfAbsent( gridPosition, k -> new ArrayList<>() )
                            .add( sourceName );
            }

            viewToGroup.put( viewName, getGroupName( row ) );
            viewToExclusive.put( viewName, getExclusive( row ) );
            viewToDisplays.computeIfAbsent( viewName, k -> new HashSet<>() ).add( displays.get( displayName ) );
            viewToTransformations.computeIfAbsent( viewName, k -> new ArrayList<>() ).addAll( getAffineTransformations( sourceName, row ) );

        } // table rows


        // Create views
        viewToDisplays.keySet().forEach( viewName ->
        {
            ArrayList< Transformation > transformations = new ArrayList<>();

            transformations.addAll( viewToTransformations.get( viewName ) );

            if ( viewToGrids.containsKey( viewName ) )
            {
                viewToGrids.get( viewName ).forEach( gridName ->
                {
                    Map< String, List< String > > positionToSources = gridToPositionsToSources.get( gridName );
                    if ( positionToSources.keySet().contains( NO_GRID_POSITION ) )
                    {
                        ArrayList< List< String > > nestedSources = new ArrayList<>( positionToSources.values() );
                        GridTransformation grid = new GridTransformation( nestedSources, "" );
                        transformations.add( grid );
                    }
                    else
                    {
                        List< int[] > positions = positionToSources.keySet().stream()
                                .map( position -> gridPositionToInts( position ) )
                                .collect( Collectors.toList() );
                        ArrayList< List< String > > nestedSources = new ArrayList<>( positionToSources.values() );

                        GridTransformation grid = new GridTransformation( nestedSources, positions, "" );
                        transformations.add( grid );
                    }

                    Display< ? > regionDisplay = createRegionDisplay( viewName, gridName, positionToSources );
                    displays.put( regionDisplay.getName(), regionDisplay );
                    viewToDisplays.get( viewName ).add( regionDisplay );
                }
            }

            viewToDisplays.get( viewName ).forEach( display ->
            {
                View view = addDisplayToView(
                        dataset,
                        viewName,
                        viewToGroup.get( viewName ),
                        viewToExclusive.get( viewName ),
                        display,
                        transformations );

                view.overlayNames( false );
            }
        }

    }

    private RegionDisplay< AnnotatedRegion > createRegionDisplay( String viewName, String gridName, List< String > gridSources )
    {
        // Create grid regions table
        Selection rowSelection = Selection
                .with( gridToRowIndices.get( viewName )
                        .stream().mapToInt( i -> i ).toArray() );
        Table regionTable = table.where( rowSelection );
        regionTable.setName( gridName + " grid" );
        regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, gridSources ) );
        final StorageLocation storageLocation = new StorageLocation();
        storageLocation.data = regionTable;
        final RegionTableSource regionTableSource = new RegionTableSource( regionTable.name() );
        regionTableSource.addTable( TableDataFormat.Table, storageLocation );
        DataStore.addRawData( regionTableSource );

        // Create RegionDisplay to show the grid
        final RegionDisplay< AnnotatedRegion > gridRegionDisplay =
                new RegionDisplay<>( regionTable.name() );
        gridRegionDisplay.sources = new LinkedHashMap<>();
        gridRegionDisplay.tableSource = regionTable.name();
        gridRegionDisplay.showAsBoundaries( true );
        gridRegionDisplay.setBoundaryThickness( 0.05 );
        gridRegionDisplay.boundaryThicknessIsRelative( true );
        gridRegionDisplay.setRelativeDilation( 2 * gridRegionDisplay.getBoundaryThickness() );

        for ( String source : gridSources )
            gridRegionDisplay.sources.put( source, Collections.singletonList( source ) );

        return gridRegionDisplay;
    }

    private void addSource( Dataset dataset, Row row, String sourceName, Map< String, Display< ? > > displays, String displayName )
    {
        String dataType = getDataType( row );

        final StorageLocation storageLocation = new StorageLocation();
        storageLocation.absolutePath = getUri( row );
        if ( rootPath != null )
            storageLocation.absolutePath = IOHelper.combinePath( rootPath, storageLocation.absolutePath );

        if ( dataType.equals( CollectionTableConstants.LABELS )  )
        {
            ImageDataFormat imageDataFormat = getImageDataFormat( row, storageLocation );
            storageLocation.setChannel( getChannelIndex( row ) ); // TODO: Fetch from table or URI? https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798

            TableSource tableSource = getTable( row, rootPath );

            SegmentationDataSource segmentationDataSource =
                    SegmentationDataSource.create(
                            sourceName,
                            imageDataFormat,
                            storageLocation,
                            tableSource
                    );

            segmentationDataSource.preInit( false );
            dataset.putDataSource( segmentationDataSource );

            if ( displays.containsKey( displayName ) )
            {
                displays.get( displayName ).getSources().add( sourceName );
            }
            else
            {
                Display< ? > display = createSegmentationDisplay(
                        sourceName,
                        row,
                        tableSource != null );

                displays.put( display.getName(), display );
            }
        }
        else if ( dataType.equals( CollectionTableConstants.SPOTS )  )
        {
            SpotDataSource spotDataSource = new SpotDataSource(
                    sourceName,
                    TableDataFormat.fromPath( storageLocation.absolutePath ),
                    storageLocation );

            double[][] boundingBox = getBoundingBox( row );
            if ( boundingBox != null )
            {
                spotDataSource.boundingBoxMin = boundingBox[ 0 ];
                spotDataSource.boundingBoxMax = boundingBox[ 1 ];
            }

            dataset.putDataSource( spotDataSource );

            if ( displays.containsKey( displayName ) )
            {
                displays.get( displayName ).getSources().add( spotDataSource.getName() );
            }
            else
            {
                SpotDisplay< AnnotatedRegion > display = createSpotDisplay( row, spotDataSource.getName() );

                displays.put( display.getName(), display );
            }
        }
        else // default: intensities
        {
            ImageDataFormat imageDataFormat = getImageDataFormat( row, storageLocation );
            storageLocation.setChannel( getChannelIndex( row ) ); // TODO: Fetch from table or URI? https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798

            final ImageDataSource imageDataSource = new ImageDataSource(
                    sourceName,
                    imageDataFormat,
                    storageLocation );
            imageDataSource.preInit( false );
            dataset.putDataSource( imageDataSource );

            if ( displays.containsKey( displayName ) )
            {
                ( ( ImageDisplay ) displays.get( displayName ) )
                        .addSource( sourceName, getContrastLimits( row ) );
            }
            else
            {
                Display< ? > display = createImageDisplay( sourceName, row );
                displays.put( display.getName(), display );
            }
        }

        IJ.log("  Name: " + sourceName );
        IJ.log("  URI: " + storageLocation.absolutePath );
        IJ.log("  Type: " + dataType );
    }

    @NotNull
    private static SpotDisplay< AnnotatedRegion > createSpotDisplay( Row row, final String spotSourceName )
    {
        SpotDisplay< AnnotatedRegion > display = new SpotDisplay<>( getDisplayName( row ) );
        display.spotRadius = getSpotRadius( row );
        display.getSources().add( spotSourceName );
        return display;
    }

    private static ImageDataFormat getImageDataFormat( Row row, StorageLocation storageLocation )
    {
        try {
            String string = row.getString( CollectionTableConstants.FORMAT );
            return ImageDataFormat.valueOf( string );
        }
        catch ( Exception e )
        {
            return ImageDataFormat.fromPath( storageLocation.absolutePath );
        }
    }

    private static boolean getExclusive( Row row )
    {
        try
        {
            return row.getBoolean( CollectionTableConstants.EXCLUSIVE );
        }
        catch ( Exception e )
        {
            try
            {
                String string = row.getText( CollectionTableConstants.EXCLUSIVE );
                if ( string.toLowerCase().equals( CollectionTableConstants.TRUE ) )
                    return true;
                else
                    return false;
            }
            catch ( Exception e2 )
            {
                return false;
            }
        }
    }

    private static TableSource getTable( Row row, String rootPath )
    {
        try {
            String tablePath = row.getString( CollectionTableConstants.LABELS_TABLE );
            if ( rootPath != null )
                tablePath = IOHelper.combinePath( rootPath, tablePath );
            StorageLocation storageLocation = new StorageLocation();
            storageLocation.absolutePath = IOHelper.getParentLocation( tablePath );
            storageLocation.defaultChunk = IOHelper.getFileName( tablePath );
            return new TableSource( TableDataFormat.fromPath( tablePath ), storageLocation );
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    private static String getUri( Row row )
    {
        String string = row.getString( CollectionTableConstants.URI );

        if ( string.isEmpty() )
            throw new RuntimeException("Encountered empty cell in uri column, please add a valid uri!");

        return string;
    }

    private static String getName( Row row )
    {
        try {
            String string = row.getString( CollectionTableConstants.NAME );
            if ( string.isEmpty() )
                return getNameFromURI( row );
            else
                return string;
        }
        catch ( Exception e )
        {
            return getNameFromURI( row );
        }
    }

    @Nullable
    private static String getNameFromURI( Row row )
    {
        String uri = getUri( row );

        String name = MoBIEHelper.removeExtension( IOHelper.getFileName( uri ) );

        Integer channel = getChannelIndex( row );
        if ( channel != null ) name = name + Constants.CHANNEL_POSTFIX + channel;

        return name;
    }

    private static String getDataType( Row row )
    {
        try
        {
            String string = row.getString( CollectionTableConstants.TYPE );
            if ( string.isEmpty() )
                return CollectionTableConstants.INTENSITIES;

            return string;
        }
        catch ( Exception e )
        {
            return CollectionTableConstants.INTENSITIES;
        }
    }

    private static String getGridId( Row row )
    {
        try {
            String string = row.getString( CollectionTableConstants.GRID );

            if ( string.isEmpty() )
                return null;

            return string;
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    private static Integer getChannelIndex( Row row )
    {
        try {
            int channelIndex = row.getInt( CollectionTableConstants.CHANNEL );
            if ( channelIndex < 0 )
                return null; // Sometimes an empty string yields a negative number
            return channelIndex;
        }
        catch ( Exception e )
        {
            return null;
        }
    }


    private static View addDisplayToView( Dataset dataset,
                                          String viewName,
                                          String groupName,
                                          boolean exclusive,
                                          Display< ? > display,
                                          List< Transformation > transformations
    )
    {
        ArrayList< Display< ? > > displays = new ArrayList<>();
        displays.add( display );

        Map< String, View > views = dataset.views();
        if ( views.containsKey( viewName ) )
        {
            View existingView = views.get( viewName );
            existingView.transformations().addAll( transformations );
            existingView.displays().addAll( displays );
            return existingView;
        }
        else
        {
            final View view = new View(
                    viewName,
                    groupName,
                    displays,
                    transformations,
                    null,
                    exclusive,
                    null );

            dataset.views().put( view.getName(), view );
            return view;
        }
    }

    @NotNull
    private static String getGroupName( Row row )
    {
        try
        {
            String name = row.getString( CollectionTableConstants.GROUP );

            if ( name.isEmpty() )
                return "views";

            return name;
        }
        catch ( Exception e )
        {
            return "views";
        }
    }

    private static String getViewName( Row row )
    {
        try
        {
            String name = row.getString( CollectionTableConstants.VIEW );

            if ( name == null || name.isEmpty() )
                return getDisplayName( row );

            return name;
        }
        catch ( Exception e )
        {
            return getDisplayName( row );
        }
    }

    @NotNull
    private static SegmentationDisplay< ? > createSegmentationDisplay(
            String sourceName,
            Row row,
            boolean showTable )
    {
        final SegmentationDisplay< ? > display =
                new SegmentationDisplay<>(
                        getDisplayName( row ),
                        new ArrayList<>( Arrays.asList( sourceName ) )
                );

        display.setBlendingMode( getBlendingMode( row ) );
        display.showTable( showTable );

        return display;
    }

    @NotNull
    private static ImageDisplay< ? > createImageDisplay( String sourceName, Row row )
    {
        return new ImageDisplay<>(
                getDisplayName( row ),
                1.0,
                new ArrayList<>( Arrays.asList( sourceName ) ),
                getColor( row ), // ColorHelper.getString( metadata.getColor() ),
                getContrastLimits( row ), //new double[]{ metadata.minIntensity(), metadata.minIntensity() }
                getBlendingMode( row ),
                false
        );
    }

    private static String getDisplayName( Row row )
    {
        if ( row.columnNames().contains( CollectionTableConstants.DISPLAY  ) )
            return row.getString( CollectionTableConstants.DISPLAY );

        if ( row.columnNames().contains( CollectionTableConstants.GRID  ) )
            return row.getString( CollectionTableConstants.GRID );

        return getName( row );
    }

    private static double[] getContrastLimits( Row row )
    {
        try
        {
            String string = row.getString( CollectionTableConstants.CONTRAST_LIMITS );
            string = string.replace("(", "").replace(")", "");
            String[] strings = string.split("[,;]");
            double[] doubles = new double[strings.length];
            for (int i = 0; i < strings.length; i++) {
                doubles[i] = Double.parseDouble(strings[i].trim());
            }

            if ( doubles.length != 2 )
                throw new UnsupportedOperationException("Contrast limits must have exactly two values: (min, max).\n" +
                        string + "does not adhere to this specification." );

            return doubles;
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    private static String getGridPosition( Row row )
    {
        try
        {
            String string = row.getString( CollectionTableConstants.GRID_POSITION );
            string = string.replace("(", "").replace(")", "");
            string = string.trim();
            return string;
        }
        catch ( Exception e )
        {
            return NO_GRID_POSITION;
        }
    }

    private static int[] gridPositionToInts( String position )
    {
        position = position.replace("(", "").replace(")", "");
        String[] strings = position.split("[,;]");
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i].trim());
        }

        if ( ints.length != 2 )
            throw new UnsupportedOperationException("Grid positions must have exactly two values: (x, y).\n" +
                    position + "does not adhere to this specification." );

        return ints;
}

    private static double[][] getBoundingBox( Row row )
    {
        try
        {
            String string = row.getString( CollectionTableConstants.BOUNDING_BOX );
            String[] minMax = string.split( "-" );
            double[][] bb = new double[ 2 ][ 3 ];
            for ( int i = 0; i < 2; i++ )
            {
                String minOrMax = minMax[i].replace("(", "").replace(")", "");
                String[] values = minOrMax.split("[,;]");
                double[] doubles = new double[3];
                for (int d = 0; d < 3; d++) {
                    doubles[ d ] = Double.parseDouble( values[ d ].trim() );
                }
                bb[ i ] = doubles;
            }
            System.out.println("Bounding box " + string );
            return bb;
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    private static double getSpotRadius( Row row )
    {
        try
        {
            return row.getNumber( CollectionTableConstants.SPOT_RADIUS );
        }
        catch ( Exception e )
        {
            return 1.0;
        }
    }

    private static BlendingMode getBlendingMode( Row row )
    {
        try
        {
            String string = row.getString( CollectionTableConstants.BLEND );

            if ( string.toLowerCase().equals( "alpha" ) )
                return BlendingMode.Alpha;

            return null;
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    // Note that this returns just a single AffineTransformation.
    // The fact that it returns a list is just for convenient consumption of
    // the downstream methods.
    private static List< Transformation > getIntensityTransformationAsList( List< String > sources, Row row )
    {
        ArrayList< Transformation > transformations = new ArrayList<>();

        try
        {
            // FIXME TODO
            String string = row.getString( CollectionTableConstants.AFFINE );
            string = string.replace("(", "").replace(")", "");
            String[] strings = string.split(",");
            double[] doubles = new double[strings.length];
            for (int i = 0; i < strings.length; i++) {
                doubles[i] = Double.parseDouble(strings[i].trim());
            }

            AffineTransformation affine = new AffineTransformation(
                    "Affine",
                    doubles,
                    sources );

            transformations.add( affine );
        }
        catch ( Exception e )
        {
           // Do not add a transformation
        }

        return transformations;
    }


    private static List< Transformation > getAffineTransformations( String sourceName, Row row )
    {
        ArrayList< Transformation > transformations = new ArrayList<>();

        try
        {
            String string = row.getString( CollectionTableConstants.AFFINE );
            string = string.replace("(", "").replace(")", "");
            String[] strings = string.split(",");
            double[] doubles = new double[strings.length];
            for (int i = 0; i < strings.length; i++) {
                doubles[i] = Double.parseDouble(strings[i].trim());
            }

            AffineTransformation affine = new AffineTransformation(
                    "Affine",
                    doubles,
                    Collections.singletonList( sourceName ) );

            transformations.add( affine );
        }
        catch ( Exception e )
        {
            // Do not add a transformation
        }

        return transformations;
    }


    private static String getColor( Row row )
    {
        try
        {
            String colorString = row.getString( CollectionTableConstants.COLOR );
            ARGBType argbType = ColorHelper.getARGBType( colorString );
            if ( argbType == null )
                return "white";

            return colorString;
        }
        catch ( Exception e )
        {
            return "white";
        }
    }

}
