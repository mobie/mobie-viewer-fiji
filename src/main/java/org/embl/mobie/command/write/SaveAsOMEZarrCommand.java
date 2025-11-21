package org.embl.mobie.command.write;

import ij.IJ;
import ij.ImagePlus;
import org.embl.mobie.command.CommandConstants;
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

    @Parameter ( label="Output folder:", style="directory",
                 description = "The parent folder where the OME-Zarr image will be created.")
    public File outputFolder;

    @Parameter ( label="Image name:",
            description = "The name of the image; for example, choosing\n" +
                    "Output folder: /usr/data\n" +
                    "Image name: my_image\n" +
                    "...will result in the creation of /usr/data/my_image.ome.zarr")
    public String imageName;

    @Parameter ( label="Image type", choices = { INTENSITY, LABEL_MASK },
                 description = "This is important, because for a \"Label mask\" (i.e., segmentations)\n" +
                         " a different downsampling method must be used\n" +
                         " in order to preserve your object identities across all resolution layers."
    )
    public String imageType;

    @Parameter ( label="Overwrite" )
    public Boolean overwrite;


    @Override
    public void run()
    {
        IJ.log("# Save as OME-Zarr...");

        //ImagePlus imp = IJ.getImage();
        IJ.log( "Dimension order: X Y C Z T");
        IJ.log( "Image dimensions: " + Arrays.toString( imp.getDimensions() ) );

        int[] chunkDimensions = new ChunkSizeComputer( imp.getDimensions(), imp.getBytesPerPixel() ).getChunkDimensionsXYCZT( 8000000 );
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
        // set pixel unit choices
        //
        final MutableModuleItem< String > pixelUnitItem = //
                getInfo().getMutableInput("imageName", String.class);

        String imageName = IJ.getImage().getTitle();
        if (imageName == null || imageName.isEmpty()) imageName = "image";
        int dotIndex = imageName.lastIndexOf('.');
        if (dotIndex > 0) imageName = imageName.substring(0, dotIndex);
        imageName = imageName.replaceAll("[\\s\\.\\\\/:;\\?\\*\\|\"'<>\\[\\]\\{\\}@!+,=]+", "--");
        imageName = imageName.replaceAll("-{2,}", "--");

        pixelUnitItem.setValue( this, imageName );
    }
}

