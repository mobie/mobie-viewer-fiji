package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenOMEZARRCommand;
import org.embl.mobie.command.open.project.OpenMoBIEProjectWithS3CredentialsCommand;
import org.embl.mobie.lib.io.DataFormats;

public class DebugSomething
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenMoBIEProjectWithS3CredentialsCommand command = new OpenMoBIEProjectWithS3CredentialsCommand();
        command.uri = "https://s3.gwdg.de/fruitfly-larva-em";
        command.s3SecretKey = "6FTq9mQwDyGvClVMvusftlMU9DhPDEr3VAJiz3MA";
        command.s3AccessKey = "FK1YVED7MTDQAMZKWS7P";
        command.location = DataFormats.Location.Remote;
        command.run();

    }
}
