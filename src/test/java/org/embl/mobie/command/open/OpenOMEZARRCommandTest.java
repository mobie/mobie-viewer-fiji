package org.embl.mobie.command.open;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OpenOMEZARRCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    //@Test
    public void test( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.containerUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";
        command.labelsUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr/labels/cells";
        command.run();
    }

    //@Test
    public void testSingleChannelAndLabels( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.containerUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";
        command.labelsUri = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr/labels/cells";
        command.run();
    }

    //@Test
    public void testMultiChannelWithLabels( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services

        final OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.containerUri = "https://uk1s3.embassy.ebi.ac.uk/idr/zarr/v0.4/idr0062A/6001240.zarr";
        command.run();
    }

    @Test
    public void opensTwiceAndAppendsViewsWhenMoBIEIsAlreadyRunning()
    {
        assumeFalse( GraphicsEnvironment.isHeadless(), "Requires a graphical environment." );

        final File omeZarr = findLocalOmeZarrResource();
        assumeTrue( omeZarr != null,
                "No local .ome.zarr test resource found under src/test/resources." );

        new ImageJ().ui().showUI(); // initialise SciJava Services

        try
        {
            final OpenOMEZARRCommand firstCommand = new OpenOMEZARRCommand();
            firstCommand.containerUri = omeZarr.getAbsolutePath();
            firstCommand.run();

            final MoBIE firstInstance = MoBIE.getInstance();
            assertNotNull( firstInstance );
            final int viewsAfterFirstOpen = firstInstance.getViews().size();

            final OpenOMEZARRCommand secondCommand = new OpenOMEZARRCommand();
            secondCommand.containerUri = omeZarr.getAbsolutePath();
            secondCommand.run();

            final MoBIE secondInstance = MoBIE.getInstance();
            assertSame( firstInstance, secondInstance,
                    "Second open should append into the existing MoBIE session." );
            assertTrue( secondInstance.getViews().size() > viewsAfterFirstOpen,
                    "Second open should create at least one additional view." );
        }
        finally
        {
            if ( MoBIE.getInstance() != null )
                MoBIE.getInstance().close();
        }
    }

    private File findLocalOmeZarrResource()
    {
        final File testResources = new File( "src/test/resources" );
        if ( ! testResources.exists() ) return null;

        final List< File > queue = new ArrayList<>();
        queue.add( testResources );
        while ( ! queue.isEmpty() )
        {
            final File current = queue.remove( queue.size() - 1 );
            final File[] children = current.listFiles();
            if ( children == null ) continue;

            for ( File child : children )
            {
                if ( child.isDirectory() )
                {
                    if ( child.getName().endsWith( ".ome.zarr" ) )
                        return child;
                    queue.add( child );
                }
            }
        }

        return null;
    }

    public static void main( String[] args )
    {
        new OpenOMEZARRCommandTest().opensTwiceAndAppendsViewsWhenMoBIEIsAlreadyRunning();
    }

}