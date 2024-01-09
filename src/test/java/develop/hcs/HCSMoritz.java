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
        new ImageJ().ui().showUI();
        new MoBIE( "/Users/tischer/Desktop/moritz/CQ1_testfiles",
                new MoBIESettings(),
                0.1,
                0.0  );
    }
}
