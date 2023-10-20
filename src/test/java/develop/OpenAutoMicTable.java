package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenAutoMicToolsTableCommand;

import java.io.File;

public class OpenAutoMicTable {
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenAutoMicToolsTableCommand command = new OpenAutoMicToolsTableCommand();
        command.table = new File( "/Volumes/almf/group/Aliaksandr/User_data/Furlong_CrispR/test_data_20231018/20231004/20231004-172458/summary_calculated1.txt" );
        command.run();
    }
}
