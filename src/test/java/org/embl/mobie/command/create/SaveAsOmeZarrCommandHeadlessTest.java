package org.embl.mobie.command.create;

import ij.IJ;
import ij.ImagePlus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless test for the OME-Zarr save command.
 *
 * The command's {@link SaveAsOMEZarrCommand#run()} itself does not pop any
 * windows — it just calls {@code OMEZarrWriter.write}. The original
 * {@code SaveAsOmeZarrCommandTest#main} only spun up an {@link net.imagej.ImageJ}
 * UI so SciJava could populate the {@code imp} parameter through the dialog;
 * here we set {@code imp} directly so the test can run headlessly.
 */
public class SaveAsOmeZarrCommandHeadlessTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Test
    public void savesIntensityImageAsOmeZarr( @TempDir Path tempDir )
    {
        File input = new File( "src/test/resources/collections/mri-stack.tif" );
        assertTrue( input.exists(), "test resource missing: " + input );

        ImagePlus imp = IJ.openImage( input.getAbsolutePath() );
        assertNotNull( imp, "could not open the test image" );

        SaveAsOMEZarrCommand cmd = new SaveAsOMEZarrCommand();
        cmd.imp = imp;
        cmd.imageName = "mri-stack-test";
        cmd.imageType = SaveAsOMEZarrCommand.INTENSITY;
        cmd.overwrite = true;
        cmd.chunkSizeMB = 50;
        cmd.outputFolder = tempDir.toFile();
        cmd.run();

        File zarrDir = new File( tempDir.toFile(), "mri-stack-test.ome.zarr" );
        assertTrue( zarrDir.exists(), "OME-Zarr container should be created on disk" );
        assertTrue( zarrDir.isDirectory(), "OME-Zarr container should be a directory" );

        // Minimal structural check: the OME-Zarr root must contain a .zattrs.
        File zattrs = new File( zarrDir, ".zattrs" );
        assertTrue( zattrs.exists(), ".zattrs (OME-Zarr metadata) is missing" );
    }

    @Test
    public void savesIntensityImageAsOmeZarr3( @TempDir Path tempDir )
    {
        File input = new File( "src/test/resources/collections/mri-stack.tif" );
        assertTrue( input.exists(), "test resource missing: " + input );

        ImagePlus imp = IJ.openImage( input.getAbsolutePath() );
        assertNotNull( imp, "could not open the test image" );

        SaveAsOMEZarrCommand cmd = new SaveAsOMEZarrCommand();
        cmd.imp = imp;
        cmd.imageName = "mri-stack-test-zarr3";
        cmd.imageType = SaveAsOMEZarrCommand.INTENSITY;
        cmd.overwrite = true;
        cmd.chunkSizeMB = 50;
        cmd.shardSizeMB = 50; // forces zarr3
        cmd.outputFolder = tempDir.toFile();
        cmd.run();

        File zarrDir = new File( tempDir.toFile(), "mri-stack-test-zarr3.ome.zarr" );
        assertTrue( zarrDir.exists(), "OME-Zarr container should be created on disk" );
        assertTrue( zarrDir.isDirectory(), "OME-Zarr container should be a directory" );

        // Minimal structural check: the OME-Zarr root must contain a .zattrs.
        File zattrs = new File( zarrDir, "zarr.json" );
        assertTrue( zattrs.exists(), "zarr.json (OME-Zarr metadata) is missing" );
    }

    @Test
    public void overwriteFlagAllowsRepeatedWrites( @TempDir Path tempDir )
    {
        File input = new File( "src/test/resources/collections/mri-stack.tif" );
        ImagePlus imp = IJ.openImage( input.getAbsolutePath() );

        SaveAsOMEZarrCommand first = new SaveAsOMEZarrCommand();
        first.imp = imp;
        first.imageName = "twice";
        first.imageType = SaveAsOMEZarrCommand.INTENSITY;
        first.overwrite = true;
        first.chunkSizeMB = 50;
        first.outputFolder = tempDir.toFile();
        first.run();

        File zarrDir = new File( tempDir.toFile(), "twice.ome.zarr" );
        assertTrue( zarrDir.exists() );
        long firstWriteSize = directorySize( zarrDir );
        assertTrue( firstWriteSize > 0 );

        // Second invocation with overwrite=true must succeed without throwing.
        SaveAsOMEZarrCommand second = new SaveAsOMEZarrCommand();
        second.imp = imp;
        second.imageName = "twice";
        second.imageType = SaveAsOMEZarrCommand.INTENSITY;
        second.overwrite = true;
        second.chunkSizeMB = 50;
        second.outputFolder = tempDir.toFile();
        second.run();

        assertTrue( zarrDir.exists() );
        // Same image written with the same settings -> roughly the same size.
        assertEquals( firstWriteSize, directorySize( zarrDir ),
                "overwriting the same image should yield the same on-disk size" );
    }

    private static long directorySize( File dir )
    {
        long total = 0;
        File[] children = dir.listFiles();
        if ( children == null ) return 0;
        for ( File f : children )
            total += f.isDirectory() ? directorySize( f ) : f.length();
        return total;
    }
}
