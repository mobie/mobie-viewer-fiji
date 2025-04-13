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
package org.embl.mobie.command.open;

import ij.IJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.imagedata.N5ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.lib.util.ThreadHelper;
import org.jetbrains.annotations.NotNull;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open OME-Zarr...")
public class OpenOMEZARRCommand implements Command {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter( label = "Image URI",
            description = "Local path or S3 address to one OME-Zarr multi-scale image."
    )
    public String imageUri; // = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";

    @Parameter( label = "( Labels URI )",
            description = "Optional. Local path or S3 address to an OME-Zarr label mask multi-scale image.",
            required = false )
    public String labelsUri; // = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr/labels/cells";

    @Parameter( label = "( Labels Table URI )",
            description = "Optional. Local path or S3 address to an table with label mask features.",
            required = false )
    public String tableUri; // = "https://raw.githubusercontent.com/mobie/platybrowser-project/refs/heads/main/data/1.0.1/tables/sbem-6dpf-1-whole-segmented-cells/default.tsv"

    @Parameter ( label = "( S3 Access Key )",
            description = "Optional. Access key for a protected S3 bucket.",
            persist = false,
            required = false )
    public String s3AccessKey;

    @Parameter ( label = "( S3 Secret Key )",
            description = "Optional. Secret key for a protected S3 bucket.",
            persist = false,
            required = false )
    public String s3SecretKey;

    @Override
    public void run() {

        final MoBIESettings settings = new MoBIESettings();

        final ArrayList< String > imageList = new ArrayList<>();
        final ArrayList< String > labelsList = new ArrayList<>();
        final ArrayList< String > tablesList = new ArrayList<>();

        if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
        {
            settings.s3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } );
        }

        if ( MoBIEHelper.notNullOrEmpty( imageUri ) )
        {
            IJ.log("Analyzing " + imageUri + "..." );
            // TODO: it is annoying that we need to open the data here
            //   and then again later when actually loading the image;
            //   we could add a cache for the imageData.
            N5ImageData< ? > n5ImageData = getN5ImageData();

            int numDataSets = n5ImageData.getNumDatasets();
            for ( int dataSetIndex = 0; dataSetIndex < numDataSets; dataSetIndex++ )
            {
                String path = n5ImageData.getPath( dataSetIndex );
                if ( path.contains( "labels" ) && MoBIEHelper.nullOrEmpty( labelsUri ) )
                    labelsList.add( imageUri + "=" + n5ImageData.getName( dataSetIndex ) + ";" + dataSetIndex );
                else
                    imageList.add( imageUri + "=" + n5ImageData.getName( dataSetIndex ) + ";" + dataSetIndex );
            }

        }

        if ( MoBIEHelper.notNullOrEmpty( labelsUri ) ) labelsList.add( labelsUri );
        if ( MoBIEHelper.notNullOrEmpty( tableUri ) ) tablesList.add( tableUri );

        try
        {
            new MoBIE( imageList, labelsList, tablesList, null, GridType.Transformed, settings );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @NotNull
    private N5ImageData< ? > getN5ImageData()
    {
        if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
            return new N5ImageData<>( imageUri, ThreadHelper.sharedQueue, new String[]{ s3AccessKey, s3SecretKey } );
        else
            return new N5ImageData<>( imageUri, ThreadHelper.sharedQueue );
    }
}
