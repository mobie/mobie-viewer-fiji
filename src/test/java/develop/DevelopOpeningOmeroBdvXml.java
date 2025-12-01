package develop;

import bdv.cache.SharedQueue;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.io.imagedata.BDVXMLImageData;
import org.embl.mobie.lib.bdv.BdvViewingMode;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DevelopOpeningOmeroBdvXml
{
    public static void main( String[] args )
    {
        //openXml();
        openCollection();
    }

    private static void openCollection()
    {
        new ImageJ().ui().showUI();

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = new File("src/test/resources/collections/omero-bdv-collection.csv").getAbsolutePath();
        command.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.UseTableFolder;
        command.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
        command.run();
    }

    private static void openXml()
    {
        new ImageJ().ui().showUI();
        System.out.println("openOmeroBdvXml...");
        // https://forum.image.sc/t/opening-omero-datasets-in-mobie/117612/22
        // usr & pwd = read-tim
        String uri = new File( "src/test/resources/collections/omero-bdv.xml" ).getAbsolutePath();
        BDVXMLImageData< ? > imageData = new BDVXMLImageData<>( uri, new SharedQueue( 1 ) );
        System.out.println( "Number of datasets: " + imageData.getNumDatasets() );
        for ( int datasetIndex = 0; datasetIndex < imageData.getNumDatasets(); datasetIndex++ )
        {
            // FIXME: improve the returned names
            System.out.println( "Dataset index, name: " + datasetIndex + ", " + imageData.getName( datasetIndex ) );
        }
        VoxelDimensions voxelDimensions = imageData.getSourcePair( 0 ).getB().getVoxelDimensions();
        assertNotNull( voxelDimensions );
        System.out.println("...done!");
    }
}
