package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.File;

class OpenOMEZarrHCSDatasetCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    //@Test
    public void test( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenHCSDatasetCommand command = new OpenHCSDatasetCommand();
        command.hcsDirectory = new File( "src/test/resources/single-plane-hcs.zarr" );
        command.run();

        // TODO: add a test assertion for ensuring the the number of channels is correct.
    }

    public static void main( String[] args )
    {
        new OpenOMEZarrHCSDatasetCommandTest().test();
    }


}