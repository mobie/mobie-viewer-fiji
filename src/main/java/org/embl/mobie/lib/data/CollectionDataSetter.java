package org.embl.mobie.lib.data;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import net.thisptr.jackson.jq.internal.misc.Strings;
import org.embl.mobie.lib.serialize.transformation.ThinPlateSplineTransformation;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.table.saw.TableOpener;
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
import org.embl.mobie.lib.serialize.transformation.DisplacementFieldTransformation;
import org.embl.mobie.lib.serialize.transformation.ElastixBSplineTransformation;
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
    public static final String REGIONS = "regions: ";
    private static final String TRANSFORM_TABLE_TYPE_COLUMN = "type";
    private static final String TRANSFORM_TABLE_VALUE_COLUMN = "value";
    private static final String TRANSFORM_TABLE_NAME_COLUMN = "name";
    private static final String TRANSFORM_TABLE_INVERT_COLUMN = "invert";
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

    public void addTableToDataset( Dataset dataset )
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
                addGridTransformationsAndRegionDisplay( dataset, viewName, transformations );
            }
            else
            {
                List< List< String > > viewSources = new ArrayList<>();
                viewToDisplays.get( viewName ).values().forEach( display ->
                {
                    display.getSources().forEach(
                            source ->
                            viewSources.add( Collections.singletonList( source) )
                    );
                } );

                // Note that the region name must be unique because it will be instantiated as an image.
                // The viewName alone may be the same as an image name, which would lead to a crash,
                // because it will "overwrite" the image.
                if ( viewSources.size() > 1 )
                {
                    Display< ? > regionDisplay = createRegionDisplay( dataset, REGIONS + viewName, viewSources, false );
                    viewToDisplays.get( viewName ).put( regionDisplay.getName(), regionDisplay );
                }
            }

            ArrayList< Display< ? > > sourceDisplays = new ArrayList<>( viewToDisplays.get( viewName ).values() );
            String[] uiSelectionGroups = viewToGroups.get( viewName );
            final View view = new View(
                    viewName,
                    uiSelectionGroups,
                    sourceDisplays,
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

    private void addGridTransformationsAndRegionDisplay( Dataset dataset, String viewName,
                                                         ArrayList< Transformation > transformations)
    {
        viewToGrids.get( viewName ).keySet().forEach( gridName ->
        {
            Map< String, List< String > > positionToSources = viewToGrids.get( viewName ).get( gridName );
            List< List< String > > nestedSources;

            if ( positionToSources.size() == 1 && positionToSources.keySet().iterator().next().equals( NO_GRID_POSITION )  )
            {
                // All sources are at the same NO_GRID_POSITION position
                // thus this is a grid without given positions

                nestedSources = positionToSources.values().iterator().next().stream()
                        .map( Collections::singletonList )
                        .collect( Collectors.toList() );

                GridTransformation grid = new GridTransformation( nestedSources, true );
                transformations.add( grid );
            }
            else
            {
                nestedSources = new ArrayList<>( positionToSources.values() );

                GridTransformation grid;
                try
                {
                    List< int[] > positions = positionToSources.keySet().stream()
                            .map( this::gridPositionToInts )
                            .collect( Collectors.toList() );
                    grid = new GridTransformation( nestedSources, positions, true );
                }
                catch ( Exception e )
                {
                    // positions were not given as integer coordinates
                    // https://github.com/mobie/mobie-viewer-fiji/issues/1276
                    grid = new GridTransformation( nestedSources,true );
                }
                grid.centerAtOrigin = true; // FIXME: should depend on something!
                transformations.add( grid );
            }

            Display< ? > regionDisplay = createRegionDisplay( dataset, REGIONS + viewName + ": " + gridName, nestedSources, true );
            viewToDisplays.get( viewName ).put( regionDisplay.getName(), regionDisplay );
        });
    }

    private RegionDisplay< AnnotatedRegion > createRegionDisplay(
            Dataset dataset,
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
//        Integer numTimePoints = sources.getMetadata().numTimePoints;
//        if ( numTimePoints == null )
//            numTimePoints = 1000; // TODO
//        for ( int t = 0; t < numTimePoints; t++ )
//            regionDisplay.timepoints().add( t );

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
        String uri = getUri( row );
        boolean isRelativePath = MoBIEHelper.isRelativePath( uri );
        if ( rootPath != null && isRelativePath )
            storageLocation.absolutePath = IOHelper.combinePath( rootPath, uri );
        else
            storageLocation.absolutePath = uri;

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

    private TableSource getTable( Row row, String rootPath )
    {
        String tablePath = getString( row, CollectionTableConstants.LABELS_TABLE_URI );

        if ( tablePath == null || tablePath.isEmpty() )
            return null;

        if ( rootPath != null )
            tablePath = IOHelper.combinePath( rootPath, tablePath );

        StorageLocation storageLocation = new StorageLocation();
        storageLocation.absolutePath = IOHelper.getParentLocation( tablePath );
        storageLocation.defaultChunk = IOHelper.getFileName( tablePath );

        IJ.log("  Labels table: " + storageLocation.defaultChunk );

        return new TableSource( TableDataFormat.fromPath( tablePath ), storageLocation );
    }

    private  String getUri( Row row )
    {
        String string = getString( row, CollectionTableConstants.URI );

        assert string != null;
        if ( string.isEmpty() )
            throw new RuntimeException("Encountered empty cell in \"uri\" column, please add a valid uri!");

        return string;
    }

    private String getString( Row row, final String... columnNames )
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

    private String getDataType( Row row )
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

    private String getGridName( Row row )
    {
        if ( row.columnNames().contains( CollectionTableConstants.GRID ) )
        {
            String gridName = getString( row, CollectionTableConstants.GRID );

            if ( gridName.isEmpty() )
                return null;
            else
                return gridName;

        }
        else if ( row.columnNames().contains( CollectionTableConstants.GRID_POSITION ) )
        {
            String gridPosition = getString( row, CollectionTableConstants.GRID_POSITION );

            if ( gridPosition.isEmpty() )
                return null;
            else
            {
                // assign non-empty grid position to default grid
                return "grid";
            }
        }
        else
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
                name = getDisplayName( row );

            return name;
        }
        catch ( Exception e )
        {
            String name = getDisplayName( row );

            return name;
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
        {
            String displayName = getString( row, CollectionTableConstants.DISPLAY );
            if ( displayName != null && !displayName.trim().isEmpty() )
                return displayName;
        }

        if ( row.columnNames().contains( CollectionTableConstants.GRID  ) )
        {
            String gridName = getString( row, CollectionTableConstants.GRID );
            if ( gridName != null && !gridName.trim().isEmpty() )
                return gridName;
        }
        
        return getDataName( row );
    }

    private double[] getContrastLimits( Row row )
    {
        try
        {
            String string = getString( row, CollectionTableConstants.CONTRAST_LIMITS );
            if ( string.equals( "auto" ) ) {
                return new double[]{0.0}; // array of length one encodes auto-contrast
            }

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

    private double[][] getBoundingBox( Row row )
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

    private BlendingMode getBlendingMode( Row row )
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
    private List< Transformation > getIntensityTransformationAsList( List< String > sources, Row row )
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

    private List< Transformation > getTransformations( String sourceName, Row row )
    {
        ArrayList< Transformation > transformations = new ArrayList<>();

        // AFFINE
        final String affineCell = getString( row, CollectionTableConstants.AFFINE );
        if ( MoBIEHelper.notNullOrEmpty( affineCell ) )
        {
            try
            {
                final double[] affineParameters = parseAffineParameters( affineCell );
                transformations.add( new AffineTransformation(
                        "Affine",
                        affineParameters,
                        Collections.singletonList( sourceName ) ) );
            }
            catch ( Exception ignored )
            {
                // If affine numbers cannot be parsed, interpret the cell as a transform table URI.
                transformations.addAll( parseTransformationsTable( sourceName, affineCell ) );
            }
        }

        // Legacy transform columns are supported but deprecated in favor of the affine transform table.
        // Displacement field
        String displacementFieldUri = getString( row, CollectionTableConstants.DISPLACEMENT_FIELD_URI );
        if ( MoBIEHelper.notNullOrEmpty( displacementFieldUri ) )
        {
            IJ.log( "WARNING: Column \"" + CollectionTableConstants.DISPLACEMENT_FIELD_URI + "\" is deprecated. Please use a transformation table referenced from \"" + CollectionTableConstants.AFFINE + "\"." );
            transformations.add( new DisplacementFieldTransformation(
                    CollectionTableConstants.DISPLACEMENT_FIELD_URI,
                    displacementFieldUri,
                    Collections.singletonList( sourceName ),
                    null ) );
        }

        // Elastix BSpline
        String elastixBSplineUri = getString( row, CollectionTableConstants.ELASTIX_BSPLINE_URI );
        if ( MoBIEHelper.notNullOrEmpty( elastixBSplineUri ) )
        {
            IJ.log( "WARNING: Column \"" + CollectionTableConstants.ELASTIX_BSPLINE_URI + "\" is deprecated. Please use a transformation table referenced from \"" + CollectionTableConstants.AFFINE + "\"." );

            ElastixBSplineTransformation elastixBSplineTransformation = new ElastixBSplineTransformation(
                    CollectionTableConstants.ELASTIX_BSPLINE_URI,
                    elastixBSplineUri,
                    Collections.singletonList( sourceName ),
                    null );
            transformations.add( elastixBSplineTransformation );
        }

        // TPS
        String tpsJSON = getString( row, CollectionTableConstants.THIN_PLATE_SPLINE_JSON );
        if ( MoBIEHelper.notNullOrEmpty( tpsJSON ) )
        {
            IJ.log( "WARNING: Column \"" + CollectionTableConstants.THIN_PLATE_SPLINE_JSON + "\" is deprecated. Please use a transformation table referenced from \"" + CollectionTableConstants.AFFINE + "\"." );

            ThinPlateSplineTransformation thinPlateSplineTransformation = new ThinPlateSplineTransformation(
                    CollectionTableConstants.THIN_PLATE_SPLINE_JSON,
                    tpsJSON,
                    Collections.singletonList( sourceName ),
                    null );

            transformations.add( thinPlateSplineTransformation );
        }

        return transformations;
    }

    private List< Transformation > parseTransformationsTable( String sourceName, String transformationTableUri )
    {
        String resolvedTransformsTableUri = resolveUri( transformationTableUri );
        final Table table = TableOpener.open( resolvedTransformsTableUri );
        final ArrayList< Transformation > transformations = new ArrayList<>();

        table.forEach( transformationRow ->
        {
            final int rowNumber = transformationRow.getRowNumber() + 1;
            final String type = getRequiredTransformTableCell( transformationRow, TRANSFORM_TABLE_TYPE_COLUMN, resolvedTransformsTableUri, rowNumber ).trim();
            final String value = getRequiredTransformTableCell( transformationRow, TRANSFORM_TABLE_VALUE_COLUMN, resolvedTransformsTableUri, rowNumber ).trim();
            final String name = getString( transformationRow, TRANSFORM_TABLE_NAME_COLUMN );
            final String transformationName = MoBIEHelper.notNullOrEmpty( name ) ? name.trim() : type;
            final boolean invert = parseTransformTableInvertCell( transformationRow, resolvedTransformsTableUri, rowNumber );

            transformations.add( createTransformation( sourceName, type, value, transformationName, invert ) );
        } );

        return transformations;
    }

    private String getRequiredTransformTableCell( Row row, String columnName, String tableUri, int rowNumber )
    {
        final String value = getString( row, columnName );
        if ( ! MoBIEHelper.notNullOrEmpty( value ) )
        {
            throw new RuntimeException( "Transformation table \"" + tableUri + "\" row " + rowNumber + " is missing required column \"" + columnName + "\"." );
        }
        return value;
    }

    private boolean parseTransformTableInvertCell( Row row, String tableUri, int rowNumber )
    {
        final String invertCell = getString( row, TRANSFORM_TABLE_INVERT_COLUMN );
        if ( ! MoBIEHelper.notNullOrEmpty( invertCell ) )
            return false;

        if ( invertCell.equalsIgnoreCase( CollectionTableConstants.TRUE ) )
            return true;

        if ( invertCell.equalsIgnoreCase( CollectionTableConstants.FALSE ) )
            return false;

        IJ.log( "WARNING: Transformation table \"" + tableUri + "\" row " + rowNumber
                + " has invalid value \"" + invertCell + "\" in optional column \""
                + TRANSFORM_TABLE_INVERT_COLUMN + "\". Supported values are \"true\" and \"false\". Using false." );

        return false;
    }

    private Transformation createTransformation( String sourceName, String type, String value, String transformationName, boolean invert )
    {
        switch ( type )
        {
            case CollectionTableConstants.AFFINE:
                if ( invert )
                    warnUnsupportedInvert( type );
                return new AffineTransformation(
                        transformationName,
                        parseAffineParameters( value ),
                        Collections.singletonList( sourceName ) );
            case CollectionTableConstants.DISPLACEMENT_FIELD_URI:
                if ( invert )
                    warnUnsupportedInvert( type );
                return new DisplacementFieldTransformation(
                        transformationName,
                        value,
                        Collections.singletonList( sourceName ),
                        null );
            case CollectionTableConstants.ELASTIX_BSPLINE_URI:
                return new ElastixBSplineTransformation(
                        transformationName,
                        value,
                        Collections.singletonList( sourceName ),
                        null,
                        invert );
            case CollectionTableConstants.THIN_PLATE_SPLINE_URI:
                if ( invert )
                    warnUnsupportedInvert( type );
                return new ThinPlateSplineTransformation(
                        transformationName,
                        value, // uri
                        Collections.singletonList( sourceName ),
                        null );
            default:
                throw new RuntimeException( "Unsupported transformation type \"" + type + "\". Supported values are \""
                        + CollectionTableConstants.AFFINE + "\", \""
                        + CollectionTableConstants.DISPLACEMENT_FIELD_URI + "\", \""
                        + CollectionTableConstants.ELASTIX_BSPLINE_URI + "\", and \""
                        + CollectionTableConstants.THIN_PLATE_SPLINE_URI + "\"." );
        }
    }

    private void warnUnsupportedInvert( String transformationType )
    {
        IJ.log( "WARNING: Transformation table column \"" + TRANSFORM_TABLE_INVERT_COLUMN + "\" is currently only supported for type \""
                + CollectionTableConstants.ELASTIX_BSPLINE_URI + "\". Ignoring invert for type \"" + transformationType + "\"." );
    }

    private double[] parseAffineParameters( String string )
    {
        string = string.replace("(", "").replace(")", "");
        String[] strings = string.split(",");
        double[] doubles = new double[ strings.length ];
        for ( int i = 0; i < strings.length; i++ )
        {
            doubles[ i ] = Double.parseDouble( strings[ i ].trim() );
        }
        return doubles;
    }

    private @Nullable String resolveUri( String uri )
    {
        if ( rootPath != null && MoBIEHelper.isRelativePath( uri ) )
            return IOHelper.combinePath( rootPath, uri );
        else
            return uri;
    }

    private String getColor( Row row )
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
