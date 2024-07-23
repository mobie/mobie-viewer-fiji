package projects.bacteria_halo_quantification;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

public class BacteriaHaloQuantification
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        String root = "/Users/tischer/Documents/bacteria-halo-quantification-data/many_images_output/";
        command.image = new File( root + ".*_intensities.tiff" );
        command.labels = new File( root + ".*_halo_labels.tiff" );
        command.table = new File( root + ".*_measurements.csv" );

        command.run();
    }
}
