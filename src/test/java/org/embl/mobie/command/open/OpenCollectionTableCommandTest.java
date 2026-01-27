package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.special.OpenSimpleCollectionTableCommand;
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
    public void excelSheet( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenSimpleCollectionTableCommand command = new OpenSimpleCollectionTableCommand();
        command.tableUri = "src/test/resources/collections/clem-collection.xlsx";
        command.run();
    }

    @Test
    public void googleSheet( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenSimpleCollectionTableCommand command = new OpenSimpleCollectionTableCommand();
        command.tableUri = "https://docs.google.com/spreadsheets/d/1d_khb5P-z1SHu09SHSS7HV0PmN_VK9ZkMKDuqF52KRg/edit?usp=sharing";
        command.run();
    }

    @Test
    public void simple()
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-table.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void largeAndSmallBlobs()
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-large-and-small-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void addImageTwice()
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-image-twice-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void alphaBlendingOrder()
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/alpha-blend-collection.csv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void clem( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/clem-example-collection.tsv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void grid( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-grid-table.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void spots2D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/spots-2d-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void nonConsecutiveSpots2D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/spots-2d-collection-non-consecutive.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void spots3D( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/spots-3d-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void spots3dWith20000Columns( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/spots-3d-20000-columns-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void singleBlobs( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void twoSameBlobs( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/two-same-blobs-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void blobsWithDates( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-with-dates-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void boatsPNG( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/boats-png-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void mrc( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/mrc-collection.txt").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void blobsAndMri( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/blobs-mri-collection.csv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void manyGroups( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/many-groups-collection.csv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void thinPlateSplineMri( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/thinplatespline-mri-collection.tsv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void thinPlateSplinePlaty( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/thinplatespline-platy-collection.tsv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }


    @Test
    public void thinPlateSplinePlatyGoogleSheet( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "https://docs.google.com/spreadsheets/d/1elair242cN5rbYtlODKu5yIscZHQ9O0Lci598-NoY4I/edit?usp=sharing";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    @Test
    public void platyGoogleSheet( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "https://docs.google.com/spreadsheets/d/1xZ4Zfpg0RUwhPZVCUrX_whB0QGztLN_VVNLx89_rZs4/edit?gid=0#gid=0";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    //@Test
    public void segmentedNuclei( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "/Users/tischer/Documents/mobie.github.io/tutorials/data/collection_tables/segmented_nuclei/collection.csv";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.TwoDimensional;
        command.run();
    }

    @Test
    public void openOrganelleGoogleSheet( )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = "https://docs.google.com/spreadsheets/d/1jEnl-0_pcOFQo8mm8SUtszoWewvjyFXY0icO7gPUaQk/edit?gid=0#gid=0";
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
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

    public static void main( String[] args )
    {
        //new OpenCollectionTableCommandTest().simple();
        //new OpenCollectionTableCommandTest().spots3D();
        //new OpenCollectionTableCommandTest().spots3dWith20000Columns();
        //new OpenCollectionTableCommandTest().createLargeSpotsTable();
        //new OpenCollectionTableCommandTest().addImageTwice();
        //new OpenCollectionTableCommandTest().clem();
        //new OpenCollectionTableCommandTest().singleBlobs();
        //new OpenCollectionTableCommandTest().mrc();
        new OpenCollectionTableCommandTest().largeAndSmallBlobs();
        //new OpenCollectionTableCommandTest().nonConsecutiveSpots2D();
        //new OpenCollectionTableCommandTest().alphaBlendingOrder();
        //new OpenCollectionTableCommandTest().blobsAndMri();
        //new OpenCollectionTableCommandTest().openOrganelleGoogleSheet();
        //new OpenCollectionTableCommandTest().segmentedNuclei();
        //new OpenCollectionTableCommandTest().thinPlateSplinePlatyGoogleSheet();
        //new OpenCollectionTableCommandTest().manyGroups();
    }
}