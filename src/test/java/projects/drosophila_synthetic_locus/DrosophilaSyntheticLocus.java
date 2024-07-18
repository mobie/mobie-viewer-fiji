package projects.drosophila_synthetic_locus;

import net.imagej.ImageJ;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.command.open.OpenTableCommand;
import org.embl.mobie.lib.transform.GridType;

import java.io.File;

public class DrosophilaSyntheticLocus
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        OpenTableCommand command = new OpenTableCommand();
        command.table = new File( "/Volumes/Image_analysis/stage_8_image_analysis/20240624-stage8/summary_.txt" );
        command.gridType = GridType.Transformed;
        command.spatialCalibration = SpatialCalibration.FromImage;
        command.images ="FileName_Result.Image_IMG";
        command.root = new File("/Volumes/Image_analysis/stage_8_image_analysis/20240624-stage8" );
        command.run();
    }
}
