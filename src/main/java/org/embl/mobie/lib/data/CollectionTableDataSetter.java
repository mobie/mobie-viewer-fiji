package org.embl.mobie.lib.data;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.DataStore;
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
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.*;

public class CollectionTableDataSetter
{
    private final Table table;
    private final String rootPath;

    private final Map< String, String > viewToGroup = new LinkedHashMap<>();

    private final Map< String, Display< ? > > gridToDisplay = new LinkedHashMap<>();
    private final Map< String, List< Transformation > > gridToTransformations = new LinkedHashMap<>();
    private final Map< String, String > gridToView = new LinkedHashMap<>();
    private final Map< String, List< Integer > > gridToRowIndices = new LinkedHashMap<>();
    private final Map< String, Boolean > gridToExclusive = new LinkedHashMap<>();

    public CollectionTableDataSetter( Table table, String rootPath )
    {
        this.table = table;
        this.rootPath = rootPath;
    }

    public void addToDataset( Dataset dataset )
    {
        if ( ! table.containsColumn( CollectionTableConstants.URI ) )
            throw new RuntimeException( "Column \"" + CollectionTableConstants.URI + "\" must be present in the collection table." );


        for ( Row row : table )
        {
            final StorageLocation storageLocation = new StorageLocation();

            storageLocation.absolutePath = getUri( row );
            if ( rootPath != null )
                storageLocation.absolutePath = IOHelper.combinePath( rootPath, storageLocation.absolutePath );

            ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( storageLocation.absolutePath );
            storageLocation.setChannel( getChannel( row ) ); // TODO: Fetch from table or URI? https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798
            String imageName = getName( row );
            String pixelType = getPixelType( row );

            Display< ? > display = null;
            if ( pixelType.equals( CollectionTableConstants.LABELS )  )
            {
                TableSource tableSource = getTable( row, rootPath );

                SegmentationDataSource segmentationDataSource =
                        SegmentationDataSource.create(
                                imageName,
                                imageDataFormat,
                                storageLocation,
                                tableSource
                        );

                segmentationDataSource.preInit( false );
                dataset.putDataSource( segmentationDataSource );

                display = createSegmentationDisplay(
                            imageName,
                            row,
                            tableSource != null );
            }
            else // intensities
            {
                final ImageDataSource imageDataSource = new ImageDataSource(
                        imageName,
                        imageDataFormat,
                        storageLocation );
                imageDataSource.preInit( false );
                dataset.putDataSource( imageDataSource );

                display = createImageDisplay(
                    imageName,
                    row );
            }

            String gridId = getGridId( row );

            if ( gridId == null )
            {
                addDisplayToView(
                        getViewName( display, row ),
                        getGroupName( display, row ),
                        getExclusive( row ),
                        display,
                        getAffineTransformationAsList( display.getSources(), row ),
                        dataset.views() );
            }
            else
            {
                // FIXME: This is for creating the gridRegionDisplay
                //    If we only want one RegionDisplay for a grid then
                //    we have an issue, because there can be multiple table rows
                //    that belong to the same region
                gridToRowIndices
                        .computeIfAbsent( gridId, k -> new ArrayList<>() )
                        .add( row.getRowNumber() );

                String gridViewName = getViewName( display, row ); // defaults to display=grid name if view name is absent
                gridToView.put( gridId, gridViewName );
                gridToExclusive.put( gridId, getExclusive( row ) );
                viewToGroup.put( gridViewName, getGroupName( display, row ) );

                if ( gridToDisplay.containsKey( gridId ) )
                {
                    // Add image to existing image display
                    Display< ? > existingDisplay = gridToDisplay.get( gridId );

                    if ( existingDisplay instanceof SegmentationDisplay )
                    {
                        existingDisplay.getSources().add( imageName );
                    }
                    else if ( existingDisplay instanceof ImageDisplay )
                    {
                        ( ( ImageDisplay ) existingDisplay ).addSource( imageName, getContrastLimits( row ) );
                    }
                }
                else
                {
                    // Register the display
                    gridToDisplay.put( gridId, display );
                }

                gridToTransformations
                        .computeIfAbsent( gridId, k -> new ArrayList<>() )
                        .addAll( getAffineTransformationAsList( Collections.singletonList( imageName ), row ) );
            }

            IJ.log(" " );
            IJ.log("Name: " + imageName );
            IJ.log("URI: " + storageLocation.absolutePath );
            IJ.log("Opener: " + imageDataFormat );
            IJ.log("Type: " + pixelType );
        }

        // Create grid views

        for ( String gridId : gridToDisplay.keySet() )
        {
            Display< ? > display = gridToDisplay.get( gridId );
            List< Transformation > transformations = gridToTransformations.get( gridId );
            GridTransformation grid = new GridTransformation( display.getSources() );
            transformations.add( grid );

            String viewName = gridToView.get( gridId );

            View gridView = addDisplayToView(
                    viewName,
                    viewToGroup.get( viewName ),
                    gridToExclusive.get( gridId ),
                    display,
                    transformations,
                    dataset.views() );

            gridView.overlayNames( false ); // TODO: exchange this with the showing the regionId column as an annotation overlay!

            // Create grid regions table
            Selection rowSelection = Selection
                    .with( gridToRowIndices.get( gridId )
                            .stream().mapToInt( i -> i ).toArray() );
            Table regionTable = table.where( rowSelection );
            regionTable.setName( display.getName() + " grid" );
            regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, display.getSources() ) );
            final StorageLocation storageLocation = new StorageLocation();
            storageLocation.data = regionTable;
            final RegionTableSource regionTableSource = new RegionTableSource( regionTable.name() );
            regionTableSource.addTable( TableDataFormat.Table, storageLocation );
            DataStore.addRawData( regionTableSource );

            // Create RegionDisplay

            final RegionDisplay< AnnotatedRegion > gridRegionDisplay =
                    new RegionDisplay<>( regionTable.name() );
            gridRegionDisplay.sources = new LinkedHashMap<>();
            gridRegionDisplay.tableSource = regionTable.name();
            gridRegionDisplay.showAsBoundaries( true );
            gridRegionDisplay.setBoundaryThickness( 0.05 );
            gridRegionDisplay.boundaryThicknessIsRelative( true );
            gridRegionDisplay.setRelativeDilation( 2 * gridRegionDisplay.getBoundaryThickness() );

            for ( String source : display.getSources() )
                gridRegionDisplay.sources.put( source, Collections.singletonList( source ) );

            // TODO: in some cases only do this once for several grids
            dataset.views().get( viewName ).displays().add( gridRegionDisplay );
        }

    }

    private static boolean getExclusive( Row row )
    {
        try {
            String string = row.getString( CollectionTableConstants.EXCLUSIVE );
            if ( string.toLowerCase().equals( CollectionTableConstants.TRUE ) )
                return true;
            else
                return false;
        }
        catch ( Exception e )
        {
            return false;
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
        return FilenameUtils.removeExtension( IOHelper.getFileName( uri ) );
    }

    private static String getPixelType( Row row )
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

    private static int getChannel( Row row )
    {
        try {
            return row.getInt( CollectionTableConstants.CHANNEL );
        }
        catch ( Exception e )
        {
            return 0;
        }
    }


    private static View addDisplayToView( String viewName,
                                          String groupName,
                                          boolean exclusive,
                                          Display< ? > display,
                                          List< Transformation > transforms,
                                          final Map< String, View > views )
    {
        ArrayList< Display< ? > > displays = new ArrayList<>();
        displays.add( display );

        if ( views.containsKey( viewName ) )
        {
            View existingView = views.get( viewName );
            existingView.transformations().addAll( transforms );
            existingView.displays().addAll( displays );
            return existingView;
        }
        else
        {
            final View newView = new View(
                    viewName,
                    groupName,
                    displays,
                    transforms,
                    null,
                    exclusive,
                    null );

            views.put( newView.getName(), newView );
            return newView;
        }
    }

    @NotNull
    private static String getGroupName( Display< ? > display, Row row )
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

    private static String getViewName( Display< ? > display, Row row )
    {
        try
        {
            String name = row.getString( CollectionTableConstants.VIEW );

            if ( name == null || name.isEmpty() )
                return display.getName();

            return name;
        }
        catch ( Exception e )
        {
            return display.getName();
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
                        getDisplayName( row, sourceName ),
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
                getDisplayName( row, sourceName ),
                1.0,
                new ArrayList<>( Arrays.asList( sourceName ) ),
                getColor( row ), // ColorHelper.getString( metadata.getColor() ),
                getContrastLimits( row ), //new double[]{ metadata.minIntensity(), metadata.minIntensity() }
                getBlendingMode( row ),
                false
        );
    }

    private static String getDisplayName( Row row, String sourceName )
    {
        if ( row.columnNames().contains( CollectionTableConstants.GRID  ) )
            return row.getString( CollectionTableConstants.GRID );

        return sourceName;
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
                        "This table cell entry does not adhere to this specification: " + string );

            return doubles;
        }
        catch ( Exception e )
        {
            return null;
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


    // Note that this returns just a single AffineTransformation.
    // The fact that it returns a list is just for convenient consumption of
    // the downstream methods.
    private static List< Transformation > getAffineTransformationAsList( List< String > sources, Row row )
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
                    sources );

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
