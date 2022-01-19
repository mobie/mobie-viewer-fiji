package org.embl.mobie.viewer.command;

import mpicbg.spim.data.SpimData;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrS3Opener;
import org.embl.mobie.viewer.view.OMEZarrViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Open OME ZARR From S3...")
public class OpenOMEZARRFromS3Command implements Command {
    @Parameter(label = "S3 URL")
    public String s3URL = "https://s3.embl.de/i2k-2020/em-raw.ome.zarr";

    protected static void openAndShow(String s3URL) throws IOException {
        SpimData spimData = OMEZarrS3Opener.readURL(s3URL);
        final OMEZarrViewer viewer = new OMEZarrViewer(spimData);
        viewer.show();
    }

    @Override
    public void run() {
        try {
            openAndShow(s3URL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
