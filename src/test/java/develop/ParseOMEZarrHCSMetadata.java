package develop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.hcs.omezarr.HCSMetadata;
import org.embl.mobie.lib.hcs.omezarr.Well;
import org.embl.mobie.lib.hcs.omezarr.WellMetadata;
import org.embl.mobie.lib.serialize.JsonHelper;

import java.io.IOException;
import java.lang.reflect.Type;

public class ParseOMEZarrHCSMetadata
{

    public static final String ZATTRS = "/.zattrs";

    public static void main( String[] args ) throws IOException
    {
        Gson gson = JsonHelper.buildGson(false);

        String url = "https://uk1s3.embassy.ebi.ac.uk/idr/zarr/v0.1/plates/5966.zarr";
        final String plateJson = IOHelper.read( url + ZATTRS );
        System.out.println( plateJson );
        HCSMetadata hcsMetadata = gson.fromJson(plateJson, new TypeToken< HCSMetadata >() {}.getType());
        String wellPath = hcsMetadata.plate.wells.get( 0 ).path;
        System.out.printf( "Site path: " + wellPath );
        String wellURL = url + "/" + wellPath ;
        final String wellJson = IOHelper.read( wellURL + ZATTRS );
        System.out.println( wellJson );

        WellMetadata wellMetadata = gson.fromJson( wellJson, new TypeToken< WellMetadata >() {}.getType() );
        String imagePath = wellMetadata.well.images.get( 0 ).path;
        String imageURL = url + "/" + wellPath + "/" + imagePath;
        final String imageJson = IOHelper.read( imageURL + ZATTRS );
        System.out.println( imageJson );
    }
}
