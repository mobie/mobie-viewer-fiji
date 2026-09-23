package org.embl.mobie.ioplugin;

import ome.zarr.fiji.open.OmeZarrOpener;
import ome.zarr.fiji.read.OmeZarr;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenOMEZARRCommand;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.net.URI;
import java.nio.file.Path;

@Plugin( type = OmeZarrOpener.class, name = OmeZarrFijiZarrOpenerPlugin.NAME,
        label = "MoBIE",
        description = "Open as a multi-resolution image in MoBIE",
        iconPath = "/mobie.png", priority = Priority.NORMAL )
public class OmeZarrFijiZarrOpenerPlugin implements OmeZarrOpener
{
    /** The stable identifier this opener is persisted under. */
    public static final String NAME = "mobie-multi-resolution";

    @Override
    public void open( final OmeZarr omeZarr )
    {
         OpenOMEZARRCommand command = new OpenOMEZARRCommand();
         command.containerUri = getCleanPath( omeZarr.uri() );
         command.run();
    }

    public static String getCleanPath( URI uri) {
        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();

        // Handle file URIs: extract the clean, OS-native path
        if ("file".equalsIgnoreCase( scheme )) {
            return Path.of( uri ).toString();
        }

        // Handle http/https (or any other network URI): return the full URI string
        return uri.toString();
    }
}
