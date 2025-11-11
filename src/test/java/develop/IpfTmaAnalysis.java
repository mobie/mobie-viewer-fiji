package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class IpfTmaAnalysis
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        //command.tableUri = "/Volumes/TMA_SHG_Run01_Run02/OME_Zarr/zarr_data_with_transformations_20251103a.csv";
        command.tableUri = "/Volumes/TMA_SHG_Run01_Run02/OME_Zarr/zarr_data_with_transformations_20251106a.csv";
        //command.tableUri = "/Users/tischer/Documents/ipf-tma-analysis/data/test/input/data_with_transformations.csv";
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.run();
    }
}
