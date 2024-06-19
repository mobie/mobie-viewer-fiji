package projects.microbial_predation;

import net.imagej.ImageJ;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;

import java.io.File;

public class MicrobialPredation
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();

//        String root = "/Users/tischer/Documents/microbial-predation-data/";
//        command.image = new File( root + "test_timelapse_subset/.*.tif" );
//        command.labels = new File( root + "test_timelapse_results/.*_labels.tif" );
//        command.table = new File( root + "test_timelapse_results/.*_measurements.csv" );
//        command.spatialCalibration = SpatialCalibration.FromTable;

        command.image = new File( "/Volumes/Microbial_Predation_Analysis_Dev/Interval_output/Source/.*.tif" );
        command.labels = new File( "/Volumes/Microbial_Predation_Analysis_Dev/Interval_output_without_profiles/.*_profile_labels.tif" );
        command.table = new File( "/Volumes/Microbial_Predation_Analysis_Dev/Interval_output_without_profiles/.*_measurements_without_profiles.csv" );
        command.spatialCalibration = SpatialCalibration.FromTable;

        command.run();

        // https://forum.image.sc/t/make-fiji-not-quit-when-launching-a-macro-headless/97814
        // run("Open Image and Labels...", "image=/Volumes/Microbial_Predation_Analysis_Dev/Interval_output/Source/.*.tif labels=/Volumes/Microbial_Predation_Analysis_Dev/Interval_output/.*_profile_labels.tif table=/Volumes/Microbial_Predation_Analysis_Dev/Interval_output/.*_measurements.csv spatialcalibration=FromTable gridtype=Transformed");
    }
}
