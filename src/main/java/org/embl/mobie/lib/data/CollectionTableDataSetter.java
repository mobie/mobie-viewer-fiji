package org.embl.mobie.lib.data;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import net.thisptr.jackson.jq.internal.misc.Strings;
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

    private final Map< String, String[] > viewToGroups = new LinkedHashMap<>();
    private final Map< String, Set< String > > viewToGrids = new LinkedHashMap<>();
    private final Map< String, Map< String, List< String > > > gridToPositionsToSources = new LinkedHashMap<>();
    private final Map< String, Set< Display< ? > > > viewToDisplays = new LinkedHashMap<>();
    private final Map< String, Boolean > viewToExclusive = new LinkedHashMap<>();
    private final Map< String, List< Transformation > > viewToTransformations = new LinkedHashMap<>();
    private final Map< String, Integer > sourceToRowIndex = new HashMap<>();

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
            // FIXME What should happen here?
            if ( sourceToRowIndex.containsKey( sourceName ) )
            {
                IJ.log( "[WARN] The collection table contains " + sourceName + "multiple times" );
            }
            sourceToRowIndex.put( sourceName, row.getRowNumber() );

            addSource( dataset, row, sourceName, displays, displayName );

            String viewName = getViewName( row );
            String gridName = getGridId( row );
            if ( gridName != null )
            {
                viewToGrids.computeIfAbsent( viewName, k -> new HashSet<>() ).add( gridName );
                String gridPosition = getGridPosition( row );
                gridToPositionsToSources
                            .computeIfAbsent( gridName, k -> new LinkedHashMap<>() )
                            .computeIfAbsent( gridPosition, k -> new ArrayList<>() )
                            .add( sourceName );
            }

            viewToGroups.put( viewName, getGroups( row ) );
            viewToExclusive.put( viewName, getExclusive( row ) );
            viewToDisplays.computeIfAbsent( viewName, k -> new LinkedHashSet<>() ).add( displays.get( displayName ) );
            viewToTransformations.computeIfAbsent( viewName, k -> new ArrayList<>() ).addAll( getAffineTransformations( sourceName, row ) );

        }); // table rows


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
                    List< List< String > > nestedSources;
                    if ( positionToSources.keySet().size() == 1 )
                    {
                        assert  positionToSources.keySet().iterator().next().equals( NO_GRID_POSITION );

                        nestedSources = positionToSources.values().iterator().next().stream()
                                .map( source -> Collections.singletonList( source ) )
                                .collect( Collectors.toList() );

                        GridTransformation grid = new GridTransformation( nestedSources, "" );
                        transformations.add( grid );
                    }
                    else
                    {
                        List< int[] > positions = positionToSources.keySet().stream()
                                .map( position -> gridPositionToInts( position ) )
                                .collect( Collectors.toList() );

                        nestedSources = new ArrayList<>( positionToSources.values() );

                        GridTransformation grid = new GridTransformation( nestedSources, positions, "" );
                        grid.centerAtOrigin = true; // FIXME: should depend on something!
                        transformations.add( grid );
                    }

                    Display< ? > regionDisplay = createRegionDisplay( gridName, nestedSources );
                    displays.put( regionDisplay.getName(), regionDisplay );
                    viewToDisplays.get( viewName ).add( regionDisplay );
                });
            }



            final View view = new View(
                    viewName,
                    viewToGroups.get( viewName ),
                    new ArrayList<>( viewToDisplays.get( viewName ) ),
                    transformations,
                    null,
                    viewToExclusive.get( viewName ),
                    null );

            view.overlayNames( false );

            dataset.views().put( view.getName(), view );

        }); // views

    }

    private RegionDisplay< AnnotatedRegion > createRegionDisplay(
            String gridName,
            List< List< String > > gridSources )
    {
        List< String > firstSources = gridSources.stream().map( sources -> sources.get( 0 ) ).collect( Collectors.toList() );

        Set< String > duplicates = MoBIEHelper.findDuplicates( firstSources );

        if ( ! duplicates.isEmpty() )
        {
            throw new UnsupportedOperationException(
                    "The grid " + gridName + "contains duplicates:\n" +
                    Strings.join( ",", duplicates ) );
        }

        // Create grid regions table
        int[] rowIndices = firstSources.stream()
                .map( source -> sourceToRowIndex.get( source ) )
                .mapToInt( Integer::intValue )
                .toArray();
        Selection rowSelection = Selection.with( rowIndices );
        Table regionTable = table.where( rowSelection );
        regionTable.setName( gridName + " grid" );
        regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, firstSources ) );
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

        for ( String source : firstSources )
            gridRegionDisplay.sources.put( source, Collections.singletonList( source ) );

        return gridRegionDisplay;
    }

    private void addSource(
            Dataset dataset,
            Row row,
            String sourceName,
            Map< String, Display< ? > > displays,
            String displayName )
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
                ( ( ImageDisplay< ? > ) displays.get( displayName ) )
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
            ImageDataFormat imageDataFormat = ImageDataFormat.valueOf( string );
            return imageDataFormat;
        }
        catch ( Exception e )
        {
            ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( storageLocation.absolutePath );
            return imageDataFormat;
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
        String name;
        try {
            name = row.getString( CollectionTableConstants.NAME );
            if ( name.isEmpty() )
                name = getNameFromURI( row );
        }
        catch ( Exception e )
        {
            name = getNameFromURI( row );
        }

        Integer channel = getChannelIndex( row );
        if ( channel != null ) name = name + Constants.CHANNEL_POSTFIX + channel;

        return name;
    }

    @Nullable
    private static String getNameFromURI( Row row )
    {
        String uri = getUri( row );

        String name = MoBIEHelper.removeExtension( IOHelper.getFileName( uri ) );

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


    @NotNull
    private static String[] getGroups( Row row )
    {
        String[] defaultValue = { "views" };

        try
        {
            String groups = row.getString( CollectionTableConstants.GROUP );

            if ( groups.isEmpty() )
                return defaultValue;

            return groups.split( "," );
        }
        catch ( Exception e )
        {
            return defaultValue;
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
