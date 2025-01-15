package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenOMEZARRCommand;
import org.embl.mobie.command.open.project.OpenMoBIEProjectCommand;
import org.embl.mobie.lib.io.DataFormats;

public class DegugMareiCredentialsIssue
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenMoBIEProjectCommand command = new OpenMoBIEProjectCommand();
        command.uri = "https://s3.gwdg.de/fruitfly-larva-em";
        command.location = DataFormats.Location.Remote;
        command.run();
    }
}
