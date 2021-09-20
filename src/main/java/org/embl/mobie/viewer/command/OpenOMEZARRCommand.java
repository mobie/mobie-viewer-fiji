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

    @Override
    public void run() {
        try {
            openAndShow( directory.toString() );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

