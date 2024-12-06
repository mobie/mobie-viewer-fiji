package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.command.open.OpenOMEZARRCommand;

import java.io.File;

public class DebugOpenCollectionTableWithCredentials
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File("/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections/standflow-table.txt");
        command.s3AccessKey = ""; // see Alexandra Mattermost
        command.s3SecretKey = "";
        command.run();
    }
}
