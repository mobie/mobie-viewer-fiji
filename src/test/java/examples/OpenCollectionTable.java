package examples;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

public class OpenCollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        openLocalTable();
        //openPlatyTable();
    }

    private static void openLocalTable()
    {
        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "src/test/resources/collections/blobs-and-spots.txt";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    /*
    * This example shows how to open a table from a Google Sheet.
    * It contains a link to a Google Sheet with a table of spots.
     */
    private static void openPlatyTable()
    {
        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        // Note that this opens sheet 1 and in sheet 2 there are some spots
        command.tableUri = "https://docs.google.com/spreadsheets/d/1xZ4Zfpg0RUwhPZVCUrX_whB0QGztLN_VVNLx89_rZs4/edit?gid=0#gid=0";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }


}