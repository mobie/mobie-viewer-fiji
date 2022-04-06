package org.embl.mobie.viewer.command;

import mpicbg.spim.data.SpimData;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrS3Opener;
import org.embl.mobie.io.util.S3Utils;
import org.embl.mobie.viewer.view.OMEZarrViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME-Zarr>" +
        "Open OME-Zarr From S3 with Credentials...")
public class OpenOMEZARRFromS3WithCredentialsCommand extends OpenOMEZARRFromS3Command {

    @Parameter ( label = "S3 Access Key", persist = false )
    public String s3AccessKey = "";

    @Parameter ( label = "S3 Secret Key", persist = false )
    public String s3SecretKey = "";

    @Override
    public void run() {
        try {
            S3Utils.setS3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } );
            openAndShow(s3URL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
