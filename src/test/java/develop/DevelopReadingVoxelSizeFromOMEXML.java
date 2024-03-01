package develop;

import loci.common.DebugTools;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;

public class DevelopReadingVoxelSizeFromOMEXML
{
    public static void main( String[] args )
    {
        DebugTools.setRootLevel( "OFF" ); // Disable Bio-Formats logging

        String filePath = "/Users/tischer/Desktop/moritz/MeasurementResult.ome.xml";

        System.out.println( FormatTools.VERSION );

        try {
            ImageProcessorReader reader = new ImageProcessorReader();
            IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
            reader.setMetadataStore(omeMeta);
            reader.setId(filePath);
            IMetadata meta = (IMetadata) reader.getMetadataStore();

            double physicalSizeX = meta.getPixelsPhysicalSizeX(1).value().doubleValue();
            double physicalSizeY = meta.getPixelsPhysicalSizeY(1).value().doubleValue();

            System.out.println("Pixel Size (X): " + physicalSizeX + " µm");
            System.out.println("Pixel Size (Y): " + physicalSizeY + " µm");

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
