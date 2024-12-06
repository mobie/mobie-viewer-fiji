package debug;

import org.embl.mobie.command.open.OpenImageAndLabelsCommand;

public class DebugOpenOpenOrganelle
{
    public static void main( String[] args )
    {
        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        command.image = "s3://janelia-cosem-datasets/jrc_hela-3/jrc_hela-3.n5/em/fibsem-uint16";
        command.run();
    }
}
