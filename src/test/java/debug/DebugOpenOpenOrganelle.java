package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;
import org.embl.mobie.lib.image.Image;

public class DebugOpenOpenOrganelle
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        command.image = "s3://janelia-cosem-datasets/jrc_hela-3/jrc_hela-3.n5/em/fibsem-uint16";
        command.run();
    }
}
