package projects.bacteria_halo_quantification;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

public class BacteriaHaloQuantification
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();

        command.image0 = new File("/Users/tischer/Documents/bacteria-halo-quantification-data/output/CV014D14_NT5102_22h_P002_19-01-23_A_spots_intensities.tiff");
        command.image1 = new File("/Users/tischer/Documents/bacteria-halo-quantification-data/output/CV014D14_NT5102_22h_P002_19-01-23_A_spots_bg_mask.tiff");

        command.labels0 = new File("/Users/tischer/Documents/bacteria-halo-quantification-data/output/CV014D14_NT5102_22h_P002_19-01-23_A_spots_halo_labels.tiff");
        command.table0 = new File("/Users/tischer/Documents/bacteria-halo-quantification-data/output/CV014D14_NT5102_22h_P002_19-01-23_A_spots_measurements.csv");

        command.run();
    }
}
