package org.embl.mobie.lib.data;

import ij.IJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.SegmentationDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.MoBIETableColumnNames;
import org.jetbrains.annotations.NotNull;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.Collections;

public class TableSourcesDataSetter
{
    private final Table table;

    public TableSourcesDataSetter( Table table )
    {
        this.table = table;
    }

    public void addToDataset( Dataset dataset )
    {
        for ( Row row : table )
        {
            final StorageLocation storageLocation = new StorageLocation();
            storageLocation.absolutePath = row.getString( MoBIETableColumnNames.URI );
            ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( storageLocation.absolutePath );
            String imageName = IOHelper.getFileName( storageLocation.absolutePath );
            storageLocation.setChannel( 0 ); // TODO: Fetch from table or URI? https://forum.image.sc/t/loading-only-one-channel-from-an-ome-zarr/97798
            // TODO: probably one needs to add the channel to the imageName
            IJ.log("## " + imageName );
            IJ.log("URI: " + storageLocation.absolutePath );
            IJ.log("Format: " + imageDataFormat );

            String pixelType = row.getString( MoBIETableColumnNames.PIXEL_TYPE );

            if ( pixelType.equals( MoBIETableColumnNames.LABELS )  )
            {
                final TableSource tableSource = null; // TODO: table path could be added to the table
                SegmentationDataSource segmentationDataSource =
                        SegmentationDataSource.create(
                                imageName,
                                imageDataFormat,
                                storageLocation,
                                tableSource );
                segmentationDataSource.preInit( false );
                dataset.addDataSource( segmentationDataSource );
            }
            else // intensities
            {
                final ImageDataSource imageDataSource = new ImageDataSource( imageName, imageDataFormat, storageLocation );
                imageDataSource.preInit( false );
                dataset.addDataSource( imageDataSource );

                final View view = createImageView( imageName );
                dataset.views().put( view.getName(), view );
            }
        }
    }

    @NotNull
    private static View createImageView( String imageName )
    {
        final ImageDisplay< ? > imageDisplay = new ImageDisplay<>(
                imageName,
                Collections.singletonList( imageName ),
                null, // ColorHelper.getString( metadata.getColor() ),
                null //new double[]{ metadata.minIntensity(), metadata.minIntensity() }
                );

        final View view = new View(
                imageName,
                "images",
                Collections.singletonList( imageDisplay ),
                null,
                null,
                false,
                null );

        return view;
    }

}
