package de.embl.cba.mobie.n5.zarr.ui;

import de.embl.cba.mobie.n5.zarr.OMEZarrReader;
import de.embl.cba.mobie.n5.zarr.OMEZarrViewer;
import mpicbg.spim.data.SpimData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Open OME ZARR from the file system...")
public class OpenOMEZARRCommand implements Command
{
    @Parameter(label = "File path ")
    public String filePath = "";

    @Override
    public void run()
    {
        try {
            openAndShow( filePath );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    protected static void openAndShow( String filePath ) throws IOException
    {
        SpimData spimData = OMEZarrReader.openFile( filePath );
        final OMEZarrViewer viewer = new OMEZarrViewer( spimData );
        viewer.show();
    }
}

