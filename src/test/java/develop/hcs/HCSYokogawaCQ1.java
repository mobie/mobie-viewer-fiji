package develop.hcs;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

public class HCSYokogawaCQ1
{
    public static void main( String[] args ) throws SpimDataException, IOException
    {
        // QUESTIONS:
        // - What is the layout of the wells (well number => row, column)
        // - What are the x y z spacings (voxel sizes)?
        // - Do you have a table already where each row is one well? We could add the number of colonies there.
        // - Is python preprocessing OK, for filtering, segmentation and object counting?
        //    - output: filtered well images, well segmentations, well table with paths

        new ImageJ().ui().showUI();

        new MoBIE( "/Users/tischer/Desktop/moritz/CQ1_testfiles",
                new MoBIESettings().removeSpatialCalibration( true ), // TODO: what is the z-spacing, how to add this?
                0.1,
                0.0  );
    }
}
