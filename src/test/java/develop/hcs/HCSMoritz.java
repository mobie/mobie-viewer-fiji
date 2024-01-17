package develop.hcs;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

public class HCSMoritz
{
    public static void main( String[] args ) throws SpimDataException, IOException
    {
        // QUESTIONS:
        // - What is the layout of the wells (numbers => position)
        // - For the raw data, what is the z-spacing? How to enter this?
        // - Ask Moritz about the pixel sizes / add an option to enter those upon loading
        // - What to do with the z positions? find the one that is most in focus?
        // - Is python preprocessing OK, for filtering, segmentation and object counting?
        //    - output: filtered well images, well segmentations, well table with paths
        // - What are the time lines? I cannot really commit to delivery dates...
        // TODO:
        // - Replace the name of the HCSPattern: Moritz -> real name:  The microscope is called CQ1 from Yokogawa.
        // - Create a GitLab repo with python code
        //    - Add conda env and instructions (astrocyte-diff could be good starting point)
        // TODO: what is the z-spacing, how to add this?

        // preprocessing:
        // data with naming scheme
        //            W0018F0001T0001Z001C1.tif
        //            W0018F0001T0001Z002C1.tif
        //            W0018F0001T0001Z003C1.tif
        //            W0018F0002T0001Z001C1.tif
        //            W0018F0002T0001Z002C1.tif
        //            W0018F0002T0001Z003C1.tif
        //            W0018F0003T0001Z001C1.tif
        //            W0018F0003T0001Z002C1.tif
        //            W0018F0003T0001Z003C1.tif
        //            W0018F0004T0001Z001C1.tif
        //            W0018F0004T0001Z002C1.tif
        //            W0018F0004T0001Z003C1.tif
        //            W0019F0001T0001Z001C1.tif
        //            W0019F0001T0001Z002C1.tif
        //            W0019F0001T0001Z003C1.tif
        //            W0019F0002T0001Z001C1.tif
        //            W0019F0002T0001Z002C1.tif
        //            W0019F0002T0001Z003C1.tif
        //            W0019F0003T0001Z001C1.tif
        //            W0019F0003T0001Z002C1.tif
        //            W0019F0003T0001Z003C1.tif
        //            W0019F0004T0001Z001C1.tif
        //            W0019F0004T0001Z002C1.tif
        //            W0019F0004T0001Z003C1.tif
        //            a.s.o. for more wells (W) .....
        // - for each well W:
        //   - for each field F:
        //      - open all z planes for this field
        //      - for each z plane: median filter with r=11, subtract the median filter from original => median_subtracted
        //      - from all the z planes (Z): only keep the median_subtracted with the highest variance => highest_variance
        //    - create one image from the 4 resulting highest_variance, putting them together like this
        //       1 2
        //       3 4
        //     => well_image
        //   - save the well_image with the name of the respective first file, e.g. W0018F0001T0001Z001C1.tif, into a new folder


        new ImageJ().ui().showUI();

        new MoBIE( "/Users/tischer/Desktop/moritz/CQ1_testfiles",
                new MoBIESettings().removeSpatialCalibration( true ), // TODO: what is the z-spacing, how to add this?
                0.1,
                0.0  );
    }
}
