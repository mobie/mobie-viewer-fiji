package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

class OpenOMEZARRCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Test
    public void test( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.image = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";
        command.labels = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr/labels/cells";
        command.run();
    }
}