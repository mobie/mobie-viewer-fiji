package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.command.open.OpenOMEZARRCommand;

public class DevelopOpenNextflowCollection
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI(); // initialises SciJava Services

        final OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "/Volumes/cba/exchange/ipf-tma-analysis-test/ome-zarr/segmentation-tischi/nf-segmentation-output-mobie.csv";
        command.run();
    }
}
