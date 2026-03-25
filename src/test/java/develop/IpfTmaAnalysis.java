package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableExpertCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class IpfTmaAnalysis
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        OpenCollectionTableExpertCommand command = new OpenCollectionTableExpertCommand();
        //command.tableUri = "/Volumes/TMA_SHG_Run01_Run02/OME_Zarr/zarr_data_with_transformations_20251103a.csv";
        command.tableUri = "/Volumes/TMA_SHG_Run01_Run02/OME_Zarr/zarr_data_with_transformations_20251106a.csv";
        command.tableUri = "/Volumes/TMA_SHG_Run01_Run02/OME_Zarr/20251117_zarr_data_with_transformations.csv";
        //command.tableUri = "/Users/tischer/Documents/ipf-tma-analysis/data/test/input/data_with_transformations.csv";
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.dataRootTypeEnum = OpenCollectionTableExpertCommand.DataRootType.UseTableFolder;
        command.run();
    }
}
