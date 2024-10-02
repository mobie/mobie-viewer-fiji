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
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.GridTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.*;

public class CollectionTableDataSetter
{
    private final Table table;
    private final String rootPath;

    private final Map< String, Display< ? > > gridToDisplay = new HashMap<>();
    private final Map< String, List< Transformation > > gridToTransformations = new HashMap<>();
    private final Map< String, String > gridToView = new HashMap<>();
    private final Map< String, String > viewToGroup = new HashMap<>();



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
                final ImageDataSource imageDataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
                imageDataSource.preInit( false );
                dataset.putDataSource( imageDataSource );

                display = createImageDisplay(
                    imageName,
                    row );
            }

            String gridId = getGridId( row );

            if ( gridId == null )
            {
                addDisplayToViews(
                        getViewName( display, row ),
                        getGroupName( display, row ),
                        display,
                        getTransformations( display.getSources(), row ),
                        dataset.views() );
            }
            else
            {
                String viewName = getViewName( display, row );
                gridToView.put( gridId, viewName );
                viewToGroup.put( viewName, getGroupName( display, row ) );

                // Add image to the grid display

                if ( gridToDisplay.containsKey( gridId ) )
                {
                    gridToDisplay.get( gridId ).getSources().add( imageName );
                }
                else
                {
                    // TODO: the display name should be the grid_id !
                    gridToDisplay.put( gridId, display );
                }

                gridToTransformations
                        .computeIfAbsent( gridId, k -> new ArrayList<>() )
                        .addAll( getTransformations( Collections.singletonList( imageName ), row ) );
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

            addDisplayToViews(
                    gridToView.get( gridId ),
                    viewToGroup.get( gridToView.get( gridId ) ),
                    display,
                    transformations,
                    dataset.views() );

            // Create region table

            Table regionTable = Table.create( display.getName() );
            regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, display.getSources() ) );
            // TODO: Add path to source (storageLocation.absolutePath)
            // regionTable.addColumns( StringColumn.create( "source_path", new ArrayList<>( nameToFullPath.values() ) ) );
            final StorageLocation storageLocation = new StorageLocation();
            storageLocation.data = regionTable;
            final RegionTableSource regionTableSource = new RegionTableSource( regionTable.name() );
            regionTableSource.addTable( TableDataFormat.Table, storageLocation );
            DataStore.addRawData( regionTableSource );

            // Create RegionDisplay

            final RegionDisplay< AnnotatedRegion > gridRegionDisplay = new RegionDisplay<>( display.getName() );
            gridRegionDisplay.sources = new LinkedHashMap<>();
            gridRegionDisplay.tableSource = regionTable.name();
            gridRegionDisplay.showAsBoundaries( true );
            gridRegionDisplay.setBoundaryThickness( 0.1 );
            gridRegionDisplay.boundaryThicknessIsRelative( true );
            gridRegionDisplay.setRelativeDilation( 0.1 );

            for ( String source : display.getSources() )
                gridRegionDisplay.sources.put( source, Collections.singletonList( source ) );



            dataset.views().get( gridToView.get( gridId ) ).displays().add( gridRegionDisplay );
        }

    }

    private static TableSource getTable( Row row, String rootPath )
    {
        try {
            String tablePath = row.getString( CollectionTableConstants.LABEL_TABLE );
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
        return row.getString( CollectionTableConstants.URI );
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
        try {
            return row.getString( CollectionTableConstants.TYPE );
        }
        catch ( Exception e )
        {
            return CollectionTableConstants.INTENSITIES;
        }
    }

    private static String getGridId( Row row )
    {
        try {
            return row.getString( CollectionTableConstants.GRID );
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


    @NotNull
    private static void addDisplayToViews( String viewName,
                                           String groupName,
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
            // if several images are combined into the
            // same view we make it exclusive
            existingView.setExclusive( true );
        }
        else
        {
            final View newView = new View(
                    viewName,
                    groupName,
                    displays,
                    transforms,
                    null,
                    false,
                    null );

            views.put( newView.getName(), newView );
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

            if ( name.isEmpty() )
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
        final ImageDisplay< ? > display = new ImageDisplay<>(
                getDisplayName( row, sourceName ),
                1.0,
                new ArrayList<>( Arrays.asList( sourceName ) ),
                getColor( row ), // ColorHelper.getString( metadata.getColor() ),
                getContrastLimits( row ), //new double[]{ metadata.minIntensity(), metadata.minIntensity() }
                getBlendingMode( row ),
                false
                );

        return display;
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
            String[] strings = string.split(",");
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

    @Nullable
    private static List< Transformation > getTransformations( List< String > sources, Row row )
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
