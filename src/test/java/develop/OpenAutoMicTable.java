package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenAutoMicTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenTableCommand command = new OpenTableCommand();
        //command.table = new File( "/Volumes/almf/group/Aliaksandr/User_data/Furlong_CrispR/test_data_20231018/20231004/20231004-172458/summary_calculated1.txt" );
        //command.table = new File( "/Volumes/almf/group/Aliaksandr/User_data/Furlong_CrispR/test_data_20231018/20231004/20231004-172458/summary_calculated1_subset.txt" );
        command.table = new File( "/Users/tischer/Desktop/teresa/summary_calculated1_subset.txt" );
        //command.table = new File( "/Volumes/cba/exchange/furlong_test/summary_calculated1_subset_zarr.txt" );
        //command.images = "Result.Image.Zarr"; // Result.Image.Zarr
        command.root = command.table.getParentFile();
        command.images = "FileName_Result.Image_IMG"; // Result.Image.Zarr
        command.run();
    }
}
