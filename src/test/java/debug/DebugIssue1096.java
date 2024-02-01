package debug;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

public class DebugIssue1096
{
    // https://github.com/mobie/mobie-viewer-fiji/issues/1096

    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        new MoBIE("/Volumes/kreshuk/hellgoth/mobie_project_shared/culture-collections", MoBIESettings.settings() );
    }
}
