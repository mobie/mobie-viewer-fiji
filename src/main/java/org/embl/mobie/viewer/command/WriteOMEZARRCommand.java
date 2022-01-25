package org.embl.mobie.viewer.command;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.n5.util.DownsampleBlock;
import org.embl.mobie.io.ome.zarr.writers.imgplus.WriteImgPlusToN5OmeZarr;
import org.embl.mobie.viewer.projectcreator.ui.ManualExportPanel;
import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import static org.embl.mobie.viewer.ui.UserInterfaceHelper.tidyString;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Export Current Image To OME-ZARR...")
public class WriteOMEZARRCommand implements Command {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter
    public ImagePlus currentImage;

    @Parameter(label="Image name:")
    public String imageName;

    @Parameter(label="Save directory:", style="directory")
    public File saveDirectory;

    @Parameter(label="Downsampling method:", choices={"Average", "Centre", "Mode"}, style="listBox")
    public String downsamplingMethod;

    @Parameter(label="Use default export parameters?")
    public boolean useDefaults = true;


    @Override
    public void run() {
        DownsampleBlock.DownsamplingMethod method = DownsampleBlock.DownsamplingMethod.valueOf(downsamplingMethod);
        String name = tidyString(imageName);
        String filePath;
        if ( !imageName.endsWith(".ome.zarr")) {
            filePath = new File(saveDirectory, imageName + ".ome.zarr").getAbsolutePath();
        } else {
            filePath = new File(saveDirectory, imageName ).getAbsolutePath();
        }

        // affine transforms are currently not supported in the ome-zarr spec, so here we just generate a default
        // identity affine
        AffineTransform3D sourceTransform = new AffineTransform3D();

        if ( name != null ) {
            if ( useDefaults ) {
                new WriteImgPlusToN5OmeZarr().export(currentImage, filePath, sourceTransform, method,
                        new GzipCompression(), new String[]{imageName});
            } else {
                new ManualExportPanel( currentImage, filePath, sourceTransform, method, imageName, ImageDataFormat.OmeZarr ).getManualExportParameters();
            }
        }
    }
}
