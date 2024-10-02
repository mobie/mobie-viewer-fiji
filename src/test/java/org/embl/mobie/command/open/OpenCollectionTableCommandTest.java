package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.File;

public class OpenCollectionTableCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Test
    public void simpleTable( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/blobs-table.txt" );
        command.dataRoot = OpenCollectionTableCommand.DataRoot.UseBelowDataRootFolder;
        command.dataRootFile = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.run();
    }

    @Test
    public void gridTable( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/blobs-grid-table.txt" );
        command.dataRoot = OpenCollectionTableCommand.DataRoot.UseBelowDataRootFolder;
        command.dataRootFile = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.run();
    }

    public static void main( String[] args )
    {
        new OpenCollectionTableCommandTest().gridTable();
    }
}