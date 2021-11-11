package org.embl.mobie.viewer.projectcreator;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class ProjectCreatorTestHelper {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    public static ImagePlus makeImage( String imageName ) {
        // make an image with random values, same size as the imagej sample head image
        return IJ.createImage(imageName, "8-bit noise", 186, 226, 27);
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
