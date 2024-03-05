/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.create;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class ProjectCreatorTestHelper {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    public static ImagePlus makeImage( String imageName, boolean is2D ) {
        // make an image with random values, same size as the imagej sample head image
        if ( !is2D ) {
            return IJ.createImage(imageName, "8-bit noise", 186, 226, 27);
        } else {
            return IJ.createImage(imageName, "8-bit noise", 186, 226, 0);
        }
    }

    public static ImagePlus makeSegmentation( String imageName ) {
        // make an image with 3 boxes with pixel values 1, 2 and 3 as mock segmentation. Same size as imagej sample
        // head image
        int width = 186;
        int height = 226;
        int depth = 27;

        ImagePlus seg = IJ.createImage(imageName, "8-bit black", width, height, depth);
        for ( int i = 1; i<depth; i++ ) {
            ImageProcessor ip = seg.getImageStack().getProcessor(i);
            ip.setValue(1);
            ip.setRoi(5, 5, 67, 25);
            ip.fill();

            ip.setValue(2);
            ip.setRoi(51, 99, 67, 25);
            ip.fill();

            ip.setValue(3);
            ip.setRoi(110, 160, 67, 25);
            ip.fill();
        }

        return seg;
    }
}
