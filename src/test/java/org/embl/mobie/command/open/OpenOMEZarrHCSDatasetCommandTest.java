package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

class OpenOMEZarrHCSDatasetCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    //@Test
    public void test( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = "src/test/resources/single-plane-hcs.zarr";
        command.run();
    }
}