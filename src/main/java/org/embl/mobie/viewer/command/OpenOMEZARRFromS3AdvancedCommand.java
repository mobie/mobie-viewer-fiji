package org.embl.mobie.viewer.command;

import de.embl.cba.n5.ome.zarr.openers.OMEZarrS3Opener;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Open OME ZARR From S3 (Advanced)..." )
public class OpenOMEZARRFromS3AdvancedCommand extends OpenOMEZARRFromS3Command
{
    @Parameter ( label = "Log chunks loading" )
    public boolean logChunkLoading = true;

    @Override
    public void run()
    {
        try
        {
            OMEZarrS3Opener.setLogChunkLoading( logChunkLoading );
            openAndShow( s3URL );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }
}
