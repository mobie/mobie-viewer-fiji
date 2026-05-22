package org.embl.mobie.command.open;

import net.imagej.ImageJ;

import java.io.File;

class OpenOMEZarrHCSDatasetCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    //@Test
    public void local()
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        // TODO: this does not work with relative path: "src/test/resources/single-plane-hcs.ome.zarr"
        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = new File( "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/single-plane-hcs.ome.zarr" );
        command.run();

        // TODO: add a test assertion for ensuring the the number of channels is correct.
    }

    public static void main( String[] args )
    {
        new OpenOMEZarrHCSDatasetCommandTest().local();
    }


}