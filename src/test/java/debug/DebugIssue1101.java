package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

public class DebugIssue1101
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
        command.image0 = new File( "/Users/tischer/Downloads/example-png-no-open/iso.*.png" );
        command.run();
    }
}
