package org.embl.mobie.lib.data;

import ij.IJ;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollectionTableDataSetter
{
    private final Table table;

    public CollectionTableDataSetter( Table table )
    {
        this.table = table;
    }

    public void addToDataset( Dataset dataset )
    {
        if ( ! table.containsColumn( CollectionTableConstants.URI ) )
            throw new RuntimeException( "Column \"" + CollectionTableConstants.URI + "\" must be present in table." );

        for ( Row row : table )
        {
            final StorageLocation storageLocation = new StorageLocation();
            storageLocation.absolutePath = getUri( row );
            ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( storageLocation.absolutePath );
            storageLocation.setChannel( getChannel( row ) ); // TODO: Fetch from table or URI? https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798
            String name = getName( row );
            String pixelType = getPixelType( row );

            if ( pixelType.equals( CollectionTableConstants.LABELS )  )
            {
                // TODO: label table path could be fetched from collection table
                final TableSource tableSource = null;

                SegmentationDataSource segmentationDataSource =
                        SegmentationDataSource.create(
                                name,
                                imageDataFormat,
                                storageLocation,
                                tableSource );

                segmentationDataSource.preInit( false );
                dataset.addDataSource( segmentationDataSource );

                final SegmentationDisplay< ? > display = createLabelsDisplay( dataName );
                addDisplayToViews( dataset, display, row );
            }
            else // intensities
            {
                final ImageDataSource imageDataSource = new ImageDataSource( name, imageDataFormat, storageLocation );
                imageDataSource.preInit( false );
                dataset.addDataSource( imageDataSource );

                ImageDisplay< ? > imageDisplay = createImageDisplay( name, row );
                addDisplayToViews( dataset, imageDisplay, row );
            }

            IJ.log("## " + name );
            IJ.log("URI: " + storageLocation.absolutePath );
            IJ.log("Format: " + imageDataFormat );
            IJ.log("Type: " + pixelType );

        }
    }

    private static String getUri( Row row )
    {
        return row.getString( CollectionTableConstants.URI );
    }

    private static String getName( Row row )
    {
        try
        {
            String name = row.getString( CollectionTableConstants.NAME );

            if ( name.isEmpty() )
                throw new UnsupportedOperationException("The name must not be empty.");

            return name;
        }
        catch ( Exception e )
        {
            String uri = getUri( row );
            String name = FilenameUtils.removeExtension( IOHelper.getFileName( uri ) );
            return name;
        }
    }

    private static String getPixelType( Row row )
    {
        try {
            return row.getString( CollectionTableConstants.PIXEL_TYPE );
        }
        catch ( Exception e )
        {
            return CollectionTableConstants.INTENSITIES;
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
    private static void addDisplayToViews( Dataset dataset, Display< ? > display, Row row )
    {
        // add view for the display of this row
        //
        String uiGroupName = ( display instanceof SegmentationDisplay ) ? CollectionTableConstants.LABELS : CollectionTableConstants.INTENSITIES;

        List< Transformation > transforms = getTransforms( display.getName(), row );
        ArrayList< Display< ? > > displays = new ArrayList<>();
        displays.add( display );

        final View view = new View(
                display.getName(),
                uiGroupName,
                displays,
                transforms, // asserting that display name == image name
                null,
                false,
                null );

        dataset.views().put( view.getName(), view );

        // if requested by the table row, add display to another view,
        // which probably will combine several displays

        String viewName = getViewName( row );
        if ( viewName == null ) return;

        if ( dataset.views().containsKey( viewName ) )
        {
            View existingView = dataset.views().get( viewName );
            existingView.transformations().addAll( transforms );
            existingView.displays().addAll( displays );
        }
        else
        {
            final View newView = new View(
                    viewName,
                    "views",
                    displays,
                    transforms, // asserting that display name == image name
                    null,
                    false,
                    null );

            dataset.views().put( newView.getName(), newView );
        }

    }

    private static String getViewName( Row row )
    {
        try
        {
            String name = row.getString( CollectionTableConstants.VIEW );
            if ( name.isEmpty() ) return null;
            return name;
        } catch ( Exception e )
        {
            return null;
        }
    }

    @NotNull
    private static SegmentationDisplay< ? > createLabelsDisplay( String name )
    {
        final SegmentationDisplay< ? > display = new SegmentationDisplay<>( name, Arrays.asList( name ) );
        return display;
    }

    @NotNull
    private static ImageDisplay< ? > createImageDisplay( String imageName, Row row )
    {
        final ImageDisplay< ? > display = new ImageDisplay<>(
                imageName,
                Collections.singletonList( imageName ),
                getColor( row ), // ColorHelper.getString( metadata.getColor() ),
                null //new double[]{ metadata.minIntensity(), metadata.minIntensity() }
                );

        return display;
    }

    @Nullable
    private static List< Transformation > getTransforms( String imageName, Row row )
    {
        ArrayList< Transformation > transformations = new ArrayList<>();

        try
        {
            String string = row.getString( CollectionTableConstants.AFFINE );
            string = string.replace("(", "").replace(")", "");
            String[] strings = string.split(", ");
            double[] doubles = new double[strings.length];
            for (int i = 0; i < strings.length; i++) {
                doubles[i] = Double.parseDouble(strings[i]);
            }

            AffineTransformation affine = new AffineTransformation(
                    "Affine",
                    doubles,
                    Collections.singletonList( imageName ) );

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
            return row.getString( CollectionTableConstants.COLOR );
        }
        catch ( Exception e )
        {
            return "White";
        }
    }

}
