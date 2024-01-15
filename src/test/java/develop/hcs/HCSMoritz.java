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
        // - What to do with the z positions? find the one that is most in focus?
        // - Is python preprocessing OK, for filtering, segmentation and object counting?
        //    - output: filtered well images, well segmentations, well table with paths
        // - What are the time lines? I cannot really commit to delivery dates...
        // TODO:
        // - Replace the name of the HCSPattern: Moritz -> real name
        // - Create a GitLab repo with python code
        //    - Add conda env and instructions (astrocyte-diff could be good starting point)
        // TODO: what is the z-spacing, how to add this?

        new ImageJ().ui().showUI();

        new MoBIE( "/Users/tischer/Desktop/moritz/CQ1_testfiles",
                new MoBIESettings().removeSpatialCalibration( true ), // TODO: what is the z-spacing, how to add this?
                0.1,
                0.0  );
    }
}
