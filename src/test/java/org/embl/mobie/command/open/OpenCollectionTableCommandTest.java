package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.junit.jupiter.api.Test;

import java.io.File;

public class OpenCollectionTableCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Test
    public void blobs( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/blobs-table.txt" );
        command.dataRootType = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingMode = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void clem( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/clem-table.txt" );
        command.dataRootType = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.run();
    }

    @Test
    public void grid( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/blobs-grid-table.txt" );
        // FIXME: Make this work with relative paths
        command.dataRootType = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingMode = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void spots2D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/spots-2d-collection.txt" );
        // FIXME: Make this work with relative paths
        command.dataRootType = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingMode = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void spots3D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.table = new File( "src/test/resources/collections/spots-3d-collection.txt" );
        // FIXME: Make this work with relative paths
        command.dataRootType = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingMode = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    public static void main( String[] args )
    {
        new OpenCollectionTableCommandTest().spots3D();
    }
}