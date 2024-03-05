/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
