package org.embl.mobie.lib.data;

import ij.IJ;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.color.ColorHelper;
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
            throw new RuntimeException( "Column \"" + CollectionTableConstants.URI + "\" must be present in the collection table." );

        for ( Row row : table )
        {
            final StorageLocation storageLocation = new StorageLocation();
            storageLocation.absolutePath = getUri( row );
            ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( storageLocation.absolutePath );
            // FIXME: how to decide?
            //imageDataFormat = ImageDataFormat.BioFormats;
            storageLocation.setChannel( getChannel( row ) ); // TODO: Fetch from table or URI? https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798
            String imageName = getName( row );
            String pixelType = getPixelType( row );

            Display< ? > display;
            if ( pixelType.equals( CollectionTableConstants.LABELS )  )
            {
                SegmentationDataSource segmentationDataSource =
                        SegmentationDataSource.create(
                                imageName,
                                imageDataFormat,
                                storageLocation,
                                null // TODO: label table path could be fetched from collection table
                        );

                segmentationDataSource.preInit( false );
                dataset.addDataSource( segmentationDataSource );

                display = createSegmentationDisplay(
                        imageName,
                        row );
            }
            else // intensities
            {
                final ImageDataSource imageDataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
                imageDataSource.preInit( false );
                dataset.addDataSource( imageDataSource );

                display = createImageDisplay(
                        imageName,
                        row );
            }

            addDisplayToViews( dataset, display, row );

            IJ.log("## " + imageName );
            IJ.log("URI: " + storageLocation.absolutePath );
            IJ.log("Opener: " + imageDataFormat );
            IJ.log("Type: " + pixelType );
        }
    }

    private static String getUri( Row row )
    {
        return row.getString( CollectionTableConstants.URI );
    }

    private static String getName( Row row )
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
        List< Transformation > transforms = getTransforms( display.getName(), row );
        ArrayList< Display< ? > > displays = new ArrayList<>();
        displays.add( display );

        String viewName = getViewName( display, row );

        if ( dataset.views().containsKey( viewName ) )
        {
            View existingView = dataset.views().get( viewName );
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
                    getGroup( display ),
                    displays,
                    transforms, // asserting that display name == image name
                    null,
                    false,
                    null );

            dataset.views().put( newView.getName(), newView );
        }

    }

    @NotNull
    private static String getGroup( Display< ? > display )
    {
        // TODO: fetch from table
        return "views";
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
    private static SegmentationDisplay< ? > createSegmentationDisplay( String name, Row row )
    {
        final SegmentationDisplay< ? > display =
                new SegmentationDisplay<>(
                        name,
                        Arrays.asList( name ) );

        display.setBlendingMode( getBlendingMode( row ) );

        return display;
    }

    @NotNull
    private static ImageDisplay< ? > createImageDisplay( String imageName, Row row )
    {
        final ImageDisplay< ? > display = new ImageDisplay<>(
                imageName,
                1.0,
                Collections.singletonList( imageName ),
                getColor( row ), // ColorHelper.getString( metadata.getColor() ),
                null, //new double[]{ metadata.minIntensity(), metadata.minIntensity() }
                getBlendingMode( row ),
                false
                );

        return display;
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
    private static List< Transformation > getTransforms( String imageName, Row row )
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
