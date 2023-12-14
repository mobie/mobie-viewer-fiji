package projects.em_xray_alignment;

import de.embl.cba.transforms.utils.Transforms;
import net.imagej.ImageJ;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.io.OpenerLogging;
import org.embl.mobie.lib.io.DataFormats;
import org.embl.mobie.lib.transform.TransformHelper;

import java.io.IOException;

public class OpenEMXRAY
{
    public static void main( String[] args ) throws IOException
    {
        OpenerLogging.setLogging( true );
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE("/Volumes/cba/exchange/em-xray-alignment/mobie" );
    }
}
