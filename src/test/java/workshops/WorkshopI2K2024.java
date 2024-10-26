package workshops;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;

import java.io.File;

public class WorkshopI2K2024
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        command.image = "/Users/tischer/Documents/cellprofiler-practical-NeuBIAS-Lisbon-2017/batch_qc_practical/Well_.*_C0.tif";
        command.labels = "/Users/tischer/Documents/cellprofiler-practical-NeuBIAS-Lisbon-2017/batch_qc_practical/Well_.*_nuclei_labels.tiff";
        command.table = "/Users/tischer/Documents/cellprofiler-practical-NeuBIAS-Lisbon-2017/batch_qc_practical/Well_.*_nuclei.txt";
        command.run();
    }
}
