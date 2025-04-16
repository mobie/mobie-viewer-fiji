package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class OpenCollectionTableCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Test
    public void simple( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri ="src/test/resources/collections/blobs-table.txt";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void clem( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri ="src/test/resources/collections/clem-table.txt";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.run();
    }

    @Test
    public void grid( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "src/test/resources/collections/blobs-grid-table.txt";
        // FIXME: Make this work with relative paths
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void spots2D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "src/test/resources/collections/spots-2d-collection.txt";
        // FIXME: Make this work with relative paths
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void spots3D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "src/test/resources/collections/spots-3d-collection.txt";
        // FIXME: Make this work with relative paths
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void spots3dWith20000Columns( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "src/test/resources/collections/spots-3d-20000-columns-collection.txt";
        // FIXME: Make this work with relative paths
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseBelowDataRootFolder;
        command.dataRoot = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections" );
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    public static void main( String[] args )
    {
        //new OpenCollectionTableCommandTest().simple();
        //new OpenCollectionTableCommandTest().spots3D();
        new OpenCollectionTableCommandTest().spots3dWith20000Columns();
        //OpenCollectionTableCommandTest.createLargeSpotsTable();
    }

    private static void createLargeSpotsTable() {
        int rows = 100;
        int columns = 20000;
        String filePath = "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections/spots_with_20000_columns.tsv";

        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write header
            writer.write("spot_id\tx\ty\tz");
            for (int i = 4; i < columns; i++) {
                writer.write("\tcol" + i);
            }
            writer.newLine();

            // Initialize random number generator
            Random random = new Random();

            // Write data
            for (int i = 1; i <= rows; i++) {
                // Write spot_id
                writer.write(Integer.toString(i));

                // Write x, y, z random values
                writer.write("\t" + random.nextDouble() * 100);
                writer.write("\t" + random.nextDouble() * 100);
                writer.write("\t" + random.nextDouble() * 100);

                // Write placeholder data for remaining columns
                for (int j = 4; j < columns; j++) {
                    writer.write("\tdata" + j);
                }

                writer.newLine();
            }

            System.out.println("Table successfully written to " + filePath);

        } catch ( IOException e) {
            throw new RuntimeException( e );
        }
    }
}