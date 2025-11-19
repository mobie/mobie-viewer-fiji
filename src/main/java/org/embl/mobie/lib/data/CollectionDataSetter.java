package org.embl.mobie.lib.data;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import net.thisptr.jackson.jq.internal.misc.Strings;
import org.embl.mobie.lib.serialize.transformation.ThinPlateSplineTransformation;
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

public class CollectionDataSetter
{
    public static final String NO_GRID_POSITION = "no grid position";
    private final Table table;
    private final String rootPath;

    private final Map< String, String[] > viewToGroups = new LinkedHashMap<>();

    // view: grid: position: sources
    private final Map< String, Map< String, Map< String, List< String > > > > viewToGrids = new LinkedHashMap<>();
    private final Map< String, Map< String, Display< ? > > > viewToDisplays = new LinkedHashMap<>();
    private final Map< String, Boolean > viewToExclusive = new LinkedHashMap<>();
    private final Map< String, List< Transformation > > viewToTransformations = new LinkedHashMap<>();
    private final Map< String, Integer > sourceToRowIndex = new HashMap<>();
    private final Map< Integer, String > rowToSourceName = new HashMap<>();

    public CollectionDataSetter( Table table, String rootPath )
    {
        this.table = table;
        this.rootPath = rootPath;
    }

    public void addToDataset( Dataset dataset )
    {
        if ( ! columnExists( CollectionTableConstants.URI ) )
            throw new RuntimeException( "Column \"" + CollectionTableConstants.URI[0] + "\" must be present in the collection table." );

        AtomicInteger sourceIndex = new AtomicInteger();
        int numRows = table.rowCount();
        table.forEach( row ->
        {
            IJ.log("Adding source " + ( sourceIndex.incrementAndGet() ) + "/" + numRows + "...");

            String sourceName = getDataName( row );
            String displayName = getDisplayName( row );
            String viewName = getViewName( row );
            String gridName = getGridName( row );
            IJ.log("  Source: " + sourceName );
            IJ.log("  Display: " + displayName );
            IJ.log("  View: " + viewName );
            if ( gridName != null ) IJ.log("  Grid: " + gridName );
            sourceToRowIndex.put( sourceName, row.getRowNumber() );

            viewToDisplays.computeIfAbsent( viewName, k -> new LinkedHashMap<>() );

            addSource( dataset, row, sourceName, viewToDisplays.get( viewName ), displayName );

            if ( gridName != null )
            {
                viewToGrids
                        .computeIfAbsent( viewName, k -> new LinkedHashMap<>() )
                        .computeIfAbsent( gridName, k -> new LinkedHashMap<>() )
                        .computeIfAbsent( getGridPosition( row ), k -> new ArrayList<>() )
                        .add( sourceName );
            }

            viewToGroups.put( viewName, getGroups( row ) );
            viewToExclusive.put( viewName, getExclusive( row ) );
            viewToTransformations.computeIfAbsent( viewName, k -> new ArrayList<>() ).addAll( getTransformations( sourceName, row ) );
        }); // table rows


        // Create views
        viewToDisplays.keySet().forEach( viewName ->
        {
            ArrayList< Transformation > transformations = new ArrayList<>();
            transformations.addAll( viewToTransformations.get( viewName ) );

            if ( viewToGrids.containsKey( viewName ) )
            {
                addGridTransformationsAndRegionDisplay( viewName, transformations );
            }
            else
            {
                List< List< String > > nestedViewSources = new ArrayList<>();
                viewToDisplays.get( viewName ).values().forEach( display ->
                {
                    display.getSources().forEach(
                            source ->
                            nestedViewSources.add( Collections.singletonList( source) )
                    );
                } );

                // Note that the regions name must be unique because it will be instantiated as an image.
                // The viewName alone may be the same as an image name, which would lead to a crash,
                // because it will "overwrite" the image.
                Display< ? > regionDisplay = createRegionDisplay( "regions: " + viewName, nestedViewSources, false );
                viewToDisplays.get( viewName ).put( regionDisplay.getName(), regionDisplay );
            }

            final View view = new View(
                    viewName,
                    viewToGroups.get( viewName ),
                    new ArrayList<>( viewToDisplays.get( viewName ).values() ),
                    transformations,
                    null,
                    viewToExclusive.get( viewName ),
                    null );

            view.overlayNames( false );

            dataset.views().put( view.getName(), view );

        }); // views

    }

    private boolean columnExists( final String... columNames )
    {
        for ( String columName : columNames )
        {
            if ( table.containsColumn( columName ) )
                return true;
        }
        return false;
    }

    private void addGridTransformationsAndRegionDisplay( String viewName,
                                                         ArrayList< Transformation > transformations)
    {
        viewToGrids.get( viewName ).keySet().forEach( gridName ->
        {
            Map< String, List< String > > positionToSources = viewToGrids.get( viewName ).get( gridName );
            List< List< String > > nestedSources;

            // FIXME: This is wrong if there are grid_positions given
            if ( positionToSources.size() == 1 )
            {
                assert positionToSources.keySet().iterator().next().equals( NO_GRID_POSITION );

                nestedSources = positionToSources.values().iterator().next().stream()
                        .map( Collections::singletonList )
                        .collect( Collectors.toList() );

                GridTransformation grid = new GridTransformation( nestedSources, "" );
                transformations.add( grid );
            }
            else
            {
                List< int[] > positions = positionToSources.keySet().stream()
                        .map( this::gridPositionToInts )
                        .collect( Collectors.toList() );

                nestedSources = new ArrayList<>( positionToSources.values() );

                GridTransformation grid = new GridTransformation( nestedSources, positions, "" );
                grid.centerAtOrigin = true; // FIXME: should depend on something!
                transformations.add( grid );
            }

            Display< ? > regionDisplay = createRegionDisplay( gridName, nestedSources, true );
            viewToDisplays.get( viewName ).put( regionDisplay.getName(), regionDisplay );
        });
    }

    private RegionDisplay< AnnotatedRegion > createRegionDisplay(
            String regionsName,
            List< List< String > > nestedSources,
            boolean isGrid )
    {
        List< String > firstSources = nestedSources.stream().map( sources -> sources.get( 0 ) ).collect( Collectors.toList() );

        Set< String > duplicates = MoBIEHelper.findDuplicates( firstSources );

        if ( ! duplicates.isEmpty() )
        {
            throw new UnsupportedOperationException(
                    "The region \"" + regionsName + "\" contains duplicates:\n" +
                    Strings.join( ", ", duplicates ) );
        }

        // Create grid regions table
        int[] rowIndices = firstSources.stream()
                .map( sourceToRowIndex::get )
                .mapToInt( Integer::intValue )
                .toArray();
        Selection rowSelection = Selection.with( rowIndices );
        Table regionTable = table.where( rowSelection );
        regionTable.setName( regionsName );
        regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, firstSources ) );
        final StorageLocation storageLocation = new StorageLocation();
        storageLocation.data = regionTable;
        final RegionTableSource regionTableSource = new RegionTableSource( regionTable.name() );
        regionTableSource.addTable( TableDataFormat.Table, storageLocation );
        DataStore.addRawData( regionTableSource );

        // Create RegionDisplay to show the grid
        final RegionDisplay< AnnotatedRegion > regionDisplay =
                new RegionDisplay<>( regionTable.name() );
        regionDisplay.sources = new LinkedHashMap<>();
        regionDisplay.tableSource = regionTable.name();

        if ( isGrid )
        {
            regionDisplay.showAsBoundaries( true );
            regionDisplay.setBoundaryThickness( 0.05 );
            regionDisplay.boundaryThicknessIsRelative( true );
            // TODO: The below "relativeDilation" is used in TableSawAnnotatedRegionCreator
            //       Here the dilation is really relative for each region.
            //       If the regions are not painted at boundaries, this relative dilation looks ugly.
            regionDisplay.setRelativeDilation( 2 * regionDisplay.getBoundaryThickness() );
        }
        else
        {
            regionDisplay.setOverlap( true );
        }

        for ( String source : firstSources )
            regionDisplay.sources.put( source, Collections.singletonList( source ) );

        return regionDisplay;
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
            String uri = storageLocation.absolutePath;

            SpotDataSource spotDataSource = new SpotDataSource(
                    sourceName,
                    TableDataFormat.fromPath( uri ),
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
    }

    @NotNull
    private SpotDisplay< AnnotatedRegion > createSpotDisplay( Row row, final String spotSourceName )
    {
        SpotDisplay< AnnotatedRegion > display = new SpotDisplay<>( getDisplayName( row ) );
        display.spotRadius = getSpotRadius( row );
        display.getSources().add( spotSourceName );
        return display;
    }

    private ImageDataFormat getImageDataFormat( Row row, StorageLocation storageLocation )
    {
        try {
            String string = getString( row, CollectionTableConstants.FORMAT );
            ImageDataFormat imageDataFormat = ImageDataFormat.valueOf( string );
            return imageDataFormat;
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
                return string.equalsIgnoreCase( CollectionTableConstants.TRUE );
            }
            catch ( Exception e2 )
            {
                return false;
            }
        }
    }

    private static TableSource getTable( Row row, String rootPath )
    {
        String tablePath = getString( row, CollectionTableConstants.LABELS_TABLE );

        if ( tablePath == null || tablePath.isEmpty() )
            return null;

        if ( rootPath != null )
            tablePath = IOHelper.combinePath( rootPath, tablePath );

        StorageLocation storageLocation = new StorageLocation();
        storageLocation.absolutePath = IOHelper.getParentLocation( tablePath );
        storageLocation.defaultChunk = IOHelper.getFileName( tablePath );

        return new TableSource( TableDataFormat.fromPath( tablePath ), storageLocation );
    }

    private  String getUri( Row row )
    {
        String string = getString( row, CollectionTableConstants.URI );

        assert string != null;
        if ( string.isEmpty() )
            throw new RuntimeException("Encountered empty cell in uri column, please add a valid uri!");

        return string;
    }

    private static String getString( Row row, final String... columnNames )
    {
        for ( String columnInRow : row.columnNames() )
        {
            for ( String columnName : columnNames )
            {
                if ( columnInRow.equalsIgnoreCase( columnName ) )
                {
                    return row.getString( columnInRow );
                }
            }
        }

        return null;
    }

    private String getDataName( Row row )
    {
        // Without that, there can be recursive errors because
        // this function is called several times for one row.
        if ( rowToSourceName.containsKey( row.getRowNumber() ) )
            return rowToSourceName.get( row.getRowNumber()  );

        String name;
        try {
            name = getString( row, CollectionTableConstants.NAME );
            if ( name.isEmpty() )
                name = createFromURI( row );
        }
        catch ( Exception e )
        {
            name = createFromURI( row );
        }

        Integer channel = getChannelIndex( row );
        if ( channel != null ) name = name + Constants.CHANNEL_POSTFIX + channel;

        if ( sourceToRowIndex.containsKey( name ) )
        {
            IJ.log( "[WARN] The collection table contains the dataset \"" + name + "\" multiple times." );
            int duplicateCount = 0;
            String originalSourceName = name;
            while ( sourceToRowIndex.containsKey( name ) )
            {
                duplicateCount++;
                name = originalSourceName + " (" + duplicateCount + ")";
            }
        }

        rowToSourceName.put( row.getRowNumber(), name );

        return name;
    }

    @Nullable
    private String createFromURI( Row row )
    {
        String uri = getUri( row );

        String name = MoBIEHelper.removeExtension( IOHelper.getFileName( uri ) );

        return name;
    }

    private static String getDataType( Row row )
    {
        try
        {
            String string = getString( row, CollectionTableConstants.TYPE );
            if ( string.isEmpty() )
                return CollectionTableConstants.INTENSITIES;

            return string;
        }
        catch ( Exception e )
        {
            return CollectionTableConstants.INTENSITIES;
        }
    }

    private static String getGridName( Row row )
    {
        try {
            String gridName = getString( row, CollectionTableConstants.GRID );

            if ( gridName.isEmpty() )
                return null;

            if ( row.columnNames().contains( CollectionTableConstants.VIEW  ) )
                return row.getString( CollectionTableConstants.VIEW  ) + ": " + gridName;

            return gridName;
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
    private String[] getGroups( Row row )
    {
        String[] defaultValue = { "views" };

        try
        {
            String groups = getString( row, CollectionTableConstants.GROUP );

            if ( groups.isEmpty() )
                return defaultValue;

            return groups.split( "," );
        }
        catch ( Exception e )
        {
            return defaultValue;
        }
    }

    private String getViewName( Row row )
    {
        try
        {
            String name = getString( row, CollectionTableConstants.VIEW );

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
    private SegmentationDisplay< ? > createSegmentationDisplay(
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
    private ImageDisplay< ? > createImageDisplay( String sourceName, Row row )
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

    private String getDisplayName( Row row )
    {
        if ( row.columnNames().contains( CollectionTableConstants.DISPLAY  ) )
            return getString( row, CollectionTableConstants.DISPLAY );

        if ( row.columnNames().contains( CollectionTableConstants.GRID  ) )
            return getString( row, CollectionTableConstants.GRID );

        if ( row.columnNames().contains( CollectionTableConstants.VIEW  ) )
            return getString( row, CollectionTableConstants.VIEW ) + ": " + getDataName( row );

        return getDataName( row );
    }

    private double[] getContrastLimits( Row row )
    {
        try
        {
            String string = getString( row, CollectionTableConstants.CONTRAST_LIMITS );
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

    private String getGridPosition( Row row )
    {
        try
        {
            String string = getString( row, CollectionTableConstants.GRID_POSITION );
            string = string.replace("(", "").replace(")", "");
            string = string.trim();
            return string;
        }
        catch ( Exception e )
        {
            return NO_GRID_POSITION;
        }
    }

    private int[] gridPositionToInts( String position )
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
            String string = getString( row, CollectionTableConstants.BOUNDING_BOX );
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
            String string = getString( row, CollectionTableConstants.BLEND );

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
            String string = getString( row, CollectionTableConstants.AFFINE );
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


    private static List< Transformation > getTransformations( String sourceName, Row row )
    {
        ArrayList< Transformation > transformations = new ArrayList<>();

        // AFFINE
        try
        {
            String string = getString( row, CollectionTableConstants.AFFINE );
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

        // TPS
        try
        {
            String string = getString( row, CollectionTableConstants.TPS );

            // FIXME: Check whether the JSON parsing works
            if ( ! string.isEmpty() )
            {
                ThinPlateSplineTransformation transformation = new ThinPlateSplineTransformation(
                        "ThinPlateSpline",
                        string,
                        Collections.singletonList( sourceName ),
                        null );

                transformations.add( transformation );
            }
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
            String colorString = getString( row, CollectionTableConstants.COLOR );
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
