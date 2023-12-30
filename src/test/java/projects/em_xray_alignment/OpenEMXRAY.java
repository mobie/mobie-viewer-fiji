package projects.em_xray_alignment;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.io.OpenerLogging;

import java.io.IOException;

public class OpenEMXRAY
{
    public static void main( String[] args ) throws IOException
    {
//        OpenerLogging.setLogging( true );
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        MoBIESettings settings = new MoBIESettings().view( "em-sift-affine--xray-u8-manual-euler" );
        new MoBIE("/Volumes/cba/exchange/em-xray-alignment/mobie", settings );
    }
}
