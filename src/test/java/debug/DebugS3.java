package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.project.OpenMoBIEProjectWithS3CredentialsCommand;
import org.embl.mobie.lib.io.DataFormats;

public class DebugS3
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenMoBIEProjectWithS3CredentialsCommand command = new OpenMoBIEProjectWithS3CredentialsCommand();
        command.uri = "https://s3.gwdg.de/fruitfly-larva-em";
        command.location = DataFormats.Location.Remote;
        command.run();

    }
}
