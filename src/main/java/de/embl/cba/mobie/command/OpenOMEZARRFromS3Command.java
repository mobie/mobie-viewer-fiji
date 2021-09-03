package de.embl.cba.mobie.command;

import de.embl.cba.n5.ome.zarr.OMEZarrViewer;
import de.embl.cba.n5.ome.zarr.openers.OMEZarrS3Opener;
import mpicbg.spim.data.SpimData;
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

    public static void main(String[] args) throws IOException {
//		final ImageJ imageJ = new ImageJ();
//		imageJ.ui().showUI();
        openAndShow("https://s3.embl.de/i2k-2020/em-raw.ome.zarr");
//		            openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/zyx.ome.zarr");

//		openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/tczyx.ome.zarr");

//		openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/cyx.ome.zarr");
//            openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/tyx.ome.zarr");
//        openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/czyx.ome.zarr");
//        openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/tzyx.ome.zarr");
//         openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/tcyx.ome.zarr");

//err
//		openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/yx.ome.zarr");
//            openAndShow("https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/flat_yx.ome.zarr");


//        openAndShow( "https://s3.embassy.ebi.ac.uk/idr/zarr/v0.1/6001240.zarr" );
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
