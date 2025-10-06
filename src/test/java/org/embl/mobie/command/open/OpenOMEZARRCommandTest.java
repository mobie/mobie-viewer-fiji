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
        command.containerUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";
        command.labelsUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr/labels/cells";
        command.run();
    }

    @Test
    public void testSingleChannelAndLabels( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.containerUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";
        command.labelsUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr/labels/cells";
        command.run();
    }

    @Test
    public void testMultiChannelWithLabels( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.containerUri = "https://uk1s3.embassy.ebi.ac.uk/idr/zarr/v0.4/idr0062A/6001240.zarr";
        command.run();
    }

    public static void main( String[] args )
    {
        new OpenOMEZARRCommandTest().testMultiChannelWithLabels();
    }

}