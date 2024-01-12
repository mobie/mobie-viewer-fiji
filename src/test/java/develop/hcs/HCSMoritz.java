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
        // - What to do with the z positions?
        // - Is python preprocessing OK, for filtering, segmentation and object counting?
        //    - output: filtered well images, well segmentations, well table with paths
        // - What are the time lines? I cannot really commit to delivery dates...
        // TODO:
        // - Replace the name of the HCSPattern: Moritz -> real name

        new ImageJ().ui().showUI();
        new MoBIE( "/Users/tischer/Desktop/moritz/CQ1_testfiles",
                new MoBIESettings(),
                0.1,
                0.0  );
    }
}
