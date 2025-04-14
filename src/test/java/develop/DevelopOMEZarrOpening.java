package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenOMEZARRCommand;

public class DevelopOMEZarrOpening
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.containerUri = "https://uk1s3.embassy.ebi.ac.uk/idr/zarr/v0.4/idr0062A/6001240.zarr";
        command.run();
    }
}
