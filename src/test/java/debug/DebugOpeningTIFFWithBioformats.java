package debug;

import ij.ImagePlus;
import org.embl.mobie.io.util.IOHelper;

public class DebugOpeningTIFFWithBioformats
{
    public static void main( String[] args )
    {
        ImagePlus imagePlusTIFF = IOHelper.openTiffFromFile( "/Users/tischer/Downloads/TBO-Test31-01-2024-10x-TL-DAPI_Plate_260/TimePoint_1/TBO-Test31-01-2024-10x-TL-DAPI_G05_s9_w2.TIF" );
        imagePlusTIFF.show();

        ImagePlus imagePlusBF = IOHelper.openWithBioFormatsFromFile( "/Users/tischer/Downloads/TBO-Test31-01-2024-10x-TL-DAPI_Plate_260/TimePoint_1/TBO-Test31-01-2024-10x-TL-DAPI_G05_s9_w2.TIF", 0 );
        imagePlusBF.show();
    }
}
