package projects.em_xray_alignment;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.io.OpenerLogging;

import java.io.IOException;

public class OpenEMXRAY
{
    public static void main( String[] args ) throws IOException
    {
//        OpenerLogging.setLogging( true );
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE("/Volumes/cba/exchange/em-xray-alignment/mobie" );
    }
}
