package issues;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenHCSDatasetCommand;

import java.io.File;

public class Issue1066
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();

        // Open HCS Plate from S3:
        // - fails with null pointer exception...why?
        // command.hcsDirectory = "https://uk1s3.embassy.ebi.ac.uk/idr/zarr/v0.1/plates/2551.zarr";

        // Open local HCS Plate
        // - does not open the labels...why?
        command.hcsDirectory = new File( "/Users/tischer/Downloads/20200812-CardiomyocyteDifferentiation14-Cycle1_mip.zarr" );
        command.run();
    }
}
