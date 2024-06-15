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

        String root = "/Volumes/Microbial_Predation_Analysis_Dev/Interval_output";
        command.image = new File( root, "Source/.*.tif" );
        command.labels = new File( root , ".*_profile_labels.tif" );
        command.table = new File( root, ".*_measurements.csv" );
        command.spatialCalibration = SpatialCalibration.FromTable;

        command.run();
    }
}
