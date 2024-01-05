/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.command.write;

import ij.ImagePlus;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.n5.shaded.*;
import org.embl.mobie.lib.create.ui.ManualExportPanel;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.n5.util.DownsampleBlock;
import org.embl.mobie.io.ome.zarr.writers.imageplus.WriteImagePlusToN5OmeZarr;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static org.embl.mobie.ui.UserInterfaceHelper.tidyString;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Create>Save Current Image as OME-ZARR...")
public class WriteOMEZARRCommand implements Command {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter
    public ImagePlus currentImage;

    @Parameter (visibility = ItemVisibility.MESSAGE, required = false)
    public String message = "Make sure your voxel calibration\n is set properly under [ Image > Properties... ]";

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
        // identity affine - it will automatically write the image scaling anyway
        AffineTransform3D sourceTransform = new AffineTransform3D();

        if ( name != null ) {
            if ( useDefaults ) {
                new WriteImagePlusToN5OmeZarr().export(currentImage, filePath, sourceTransform, method,
                        new GzipCompression());
            } else {
                ManualExportPanel manualExportPanel = new ManualExportPanel( ImageDataFormat.OmeZarr );
                int[][] resolutions = manualExportPanel.getResolutions();
                int[][] subdivisions = manualExportPanel.getSubdivisions();
                Compression compression = manualExportPanel.getCompression();

                if ( resolutions != null && subdivisions != null && compression != null ) {
                    new WriteImagePlusToN5OmeZarr().export(currentImage, resolutions, subdivisions, filePath, sourceTransform,
                            method, compression );
                }
            }
        }
    }
}
