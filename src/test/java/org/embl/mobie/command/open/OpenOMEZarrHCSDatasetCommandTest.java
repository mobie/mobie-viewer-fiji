package org.embl.mobie.command.open;

import net.imagej.ImageJ;

class OpenOMEZarrHCSDatasetCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    public static void main( String[] args )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = "src/test/resources/single-plane-hcs.zarr";
        command.run();
    }
}