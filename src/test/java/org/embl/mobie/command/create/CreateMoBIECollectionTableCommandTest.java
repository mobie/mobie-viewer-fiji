package org.embl.mobie.command.create;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;

import java.io.File;
import java.io.IOException;

public class CreateMoBIECollectionTableCommandTest
{
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        CreateMoBIECollectionTableCommand createCommand = new CreateMoBIECollectionTableCommand();
        File imageFile0 = new File( "src/test/resources/collections/control--000.tif" );
        File imageFile1 = new File( "src/test/resources/collections/treated--000.tif" );
        createCommand.files = new File[]{ imageFile0, imageFile1 };
        createCommand.outputTableFile = new File("src/test/resources/collections/mobie-collection.csv");
        createCommand.openTableInMoBIE = false;
        createCommand.viewLayout = CreateMoBIECollectionTableCommand.GRID;
        createCommand.run();

        OpenCollectionTableCommand openCommand = new OpenCollectionTableCommand();
        openCommand.tableUri = createCommand.outputTableFile.getAbsolutePath();
        openCommand.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        openCommand.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        openCommand.run();
    }
}