package projects.bacteria_halo_quantification;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsFilesCommand;

import java.io.File;

public class BacteriaHaloQuantification
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenMultipleImagesAndLabelsFilesCommand command = new OpenMultipleImagesAndLabelsFilesCommand();

        String root = "/Users/tischer/Documents/bacteria-halo-quantification-data/many_images_output/";
        //String root = "/Users/tischer/Documents/bacteria-halo-quantification-data/few_images_output/";
        command.image0 = new File( root + ".*_intensities.tiff" );
        //command.image1 = new File( root + ".*_bg_mask.tiff" );

        command.labels0 = new File( root + ".*_halo_labels.tiff" );
        command.table0 = new File( root + ".*_measurements.csv" );

        command.run();
    }
}
