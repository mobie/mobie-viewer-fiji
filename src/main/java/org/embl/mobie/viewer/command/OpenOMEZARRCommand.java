package org.embl.mobie.viewer.command;


import de.embl.cba.n5.ome.zarr.OMEZarrViewer;
import de.embl.cba.n5.ome.zarr.openers.OMEZarrOpener;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Open OME ZARR From File System...")
public class OpenOMEZARRCommand implements Command {
    @Parameter(label = "File path", style = "directory")
    public File directory;

    protected static void openAndShow(String filePath) throws IOException {
        SpimData spimData = OMEZarrOpener.openFile(filePath);
        final OMEZarrViewer viewer = new OMEZarrViewer(spimData);
        viewer.show();
    }

    public static void main(String[] args) {
        try {
//            openAndShow("/home/katerina/Documents/data/v0.3/yx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/v0.3/flat_yx.ome.zarr");
//
//            openAndShow("/home/katerina/Documents/data/v0.3/cyx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/v0.3/tyx.ome.zarr");

            openAndShow("/home/katerina/Documents/data/v0.3/zyx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/v0.3/czyx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/v0.3/tzyx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/v0.3/tcyx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/v0.3/tczyx.ome.zarr");
//            openAndShow("/home/katerina/Documents/data/Covid19-S4-Area2/images/bdv.ome.zarr/raw.ome.zarr");
//            openAndShow("/home/katerina/Downloads/example.ome.zarr");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            openAndShow( directory.toString() );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

