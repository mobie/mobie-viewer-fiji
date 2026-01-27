package org.embl.mobie.command.create;

import ij.IJ;
import ij.ImagePlus;
import org.embl.mobie.io.OMEZarrWriter;
import org.embl.mobie.io.util.ChunkSizeComputer;
import org.embl.mobie.io.util.IOHelper;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Arrays;

@Plugin(type = Command.class, menuPath = "Plugins > OME-Zarr > Save Current Image as OME-Zarr..." )
public class SaveAsOMEZarrCommand extends DynamicCommand implements Initializable
{
    public static final String LABEL_MASK = "Label mask";
    public static final String INTENSITY = "Intensity";

    static { net.imagej.patcher.LegacyInjector.preinit(); }
    
    @Parameter
    public ImagePlus imp;

    @Parameter ( label="Output folder", style="directory",
                 description = "The parent folder where the OME-Zarr image will be created.")
    public File outputFolder;

    @Parameter ( label="Image name",
                 description = "The name of the image; for example, choosing\n" +
                    "Output folder: /usr/data\n" +
                    "Image name: my_image\n" +
                    "...will result in the creation of /usr/data/my_image.ome.zarr")
    public String imageName;

    @Parameter ( label="Image content type", choices = { INTENSITY, LABEL_MASK },
                 description = "This is important, because for a \"Label mask\" (i.e., segmentations)\n" +
                         " a different downsampling method must be used\n" +
                         " in order to preserve your object identities across all resolution layers."
    )
    public String imageType;

    @Parameter ( label="Chunk size (MB)",
            description = "Smaller chunk sizes can help to more efficiently lazy-load parts of the dataset;\n" +
                    "however, going too small might actually decrease the performance due I/O overhead,\n" +
                    "and there can be very many chunks (files), which can cause a problem for your file system.\n" +
                    "It is considered that chunk sizes on disk between 1 and 10 MB is reasonable.\n" +
                    "Since the chunks are compressed we recommend selecting here large chunk sizes, e.g. 50 MB." )
    public int chunkSizeMB = 50;

    @Parameter ( label="Overwrite" )
    public Boolean overwrite;

    @Override
    public void run()
    {
        IJ.log( "# Save as OME-Zarr..." );
        IJ.log( "Dimension order: X Y C Z T" );
        IJ.log( "Image dimensions: " + Arrays.toString( imp.getDimensions() ) );

        // https://forum.image.sc/t/should-compression-play-a-role-in-selecting-chunk-sizes-for-ome-zarr-v0-4-datasets/117877
        int[] chunkDimensions = new ChunkSizeComputer( imp.getDimensions(), imp.getBytesPerPixel() )
                .getChunkDimensionsXYCZT( chunkSizeMB * 1000000 );
        IJ.log( "Chunk dimensions: " + Arrays.toString( chunkDimensions ) );

        String omeXml = IOHelper.getOMEXml( imp );
        if ( omeXml != null )
            IJ.log( "OME-XML metadata will be transferred to OME-Zarr." );

        OMEZarrWriter.ImageType type = imageType.equals( LABEL_MASK ) ? OMEZarrWriter.ImageType.Labels : OMEZarrWriter.ImageType.Intensities;

        String uri = IOHelper.combinePath( outputFolder.getAbsolutePath(), imageName + ".ome.zarr" );

        OMEZarrWriter.write(
                imp,
                uri,
                type,
                chunkDimensions,
                overwrite,
                omeXml);

        IJ.log( "OME-Zarr created at: " + uri );
    }

    @Override
    public void initialize() {
        final MutableModuleItem< String > imageNameItem = getInfo().getMutableInput("imageName", String.class);

        // fetch image title
        String imageName = imp.getTitle();
        if (imageName == null || imageName.isEmpty()) imageName = "image";

        // remove file extension
        int dotIndex = imageName.lastIndexOf('.');
        if (dotIndex > 0) imageName = imageName.substring(0, dotIndex);

        // replace bad characters with "--"
        imageName = imageName.replaceAll("[\\s\\.\\\\/:;\\?\\*\\|\"'<>\\[\\]\\{\\}@!+,=]+", "--");
        imageName = imageName.replaceAll("-{2,}", "--");

        imageNameItem.setValue( this, imageName );
    }
}

