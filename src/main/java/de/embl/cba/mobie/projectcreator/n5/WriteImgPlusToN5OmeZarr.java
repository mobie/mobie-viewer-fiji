package de.embl.cba.mobie.projectcreator.n5;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.spimdata.SequenceDescriptionMinimal;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.Compression;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.*;

public class WriteImgPlusToN5OmeZarr extends WriteImgPlusToN5 {

    // TODO - deal with transforms properly - is there somewhere in ome-zarr this can be written?

    // export, generating default source transform, and default resolutions / subdivisions
    @Override
    public void export( ImagePlus imp, String zarrPath, DownsampleBlock.DownsamplingMethod downsamplingMethod,
                        Compression compression ) {
        super.export( imp, zarrPath, downsamplingMethod, compression );
    }

    // export, generating default resolutions / subdivisions
    @Override
    public void export(ImagePlus imp, String zarPath, AffineTransform3D sourceTransform,
                       DownsampleBlock.DownsamplingMethod downsamplingMethod, Compression compression ) {
        super.export( imp, zarPath, sourceTransform, downsamplingMethod, compression );
    }


    // export, generating default resolutions / subdivisions
    @Override
    public void export(ImagePlus imp, String zarrPath, AffineTransform3D sourceTransform,
                       DownsampleBlock.DownsamplingMethod downsamplingMethod, Compression compression,
                       String[] viewSetupNames ) {
        super.export( imp, zarrPath, sourceTransform, downsamplingMethod, compression, viewSetupNames );
    }

    @Override
    public void export( ImagePlus imp, int[][] resolutions, int[][] subdivisions, String zarrPath,
                        AffineTransform3D sourceTransform, DownsampleBlock.DownsamplingMethod downsamplingMethod,
                        Compression compression ) {
        export( imp, resolutions, subdivisions, zarrPath, sourceTransform, downsamplingMethod, compression, null );
    }

    @Override
    public void export( ImagePlus imp, int[][] resolutions, int[][] subdivisions, String zarrPath,
                        AffineTransform3D sourceTransform, DownsampleBlock.DownsamplingMethod downsamplingMethod,
                        Compression compression, String[] viewSetupNames ) {
        if ( resolutions.length == 0 ) {
            IJ.showMessage( "Invalid resolutions - length 0" );
            return;
        }

        if ( subdivisions.length == 0 ) {
            IJ.showMessage( " Invalid subdivisions - length 0" );
            return;
        }

        if ( resolutions.length != subdivisions.length ) {
            IJ.showMessage( "Subsampling factors and chunk sizes must have the same number of elements" );
            return;
        }

        final File zarrFile = new File( zarrPath );

        // TODO - check transform and downsampling mode

        Parameters exportParameters = new Parameters( resolutions, subdivisions, null, zarrFile, sourceTransform,
                downsamplingMethod, compression, viewSetupNames );

        export( imp, exportParameters );
    }

    @Override
    protected Parameters generateDefaultParameters(ImagePlus imp, String zarrPath, AffineTransform3D sourceTransform,
                                                   DownsampleBlock.DownsamplingMethod downsamplingMethod, Compression compression,
                                                   String[] viewSetupNames ) {
        FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        FinalDimensions size = getSize( imp );

        // propose reasonable mipmap settings
        final int maxNumElements = 64 * 64 * 64;
        final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps(
                new BasicViewSetup(0, "", size, voxelSize),
                maxNumElements);

        int[][] resolutions = autoMipmapSettings.getExportResolutions();
        int[][] subdivisions = autoMipmapSettings.getSubdivisions();

        if ( resolutions.length == 0 || subdivisions.length == 0 || resolutions.length != subdivisions.length ) {
            IJ.showMessage( "Error with calculating default subdivisions and resolutions");
            return null;
        }

        final File zarrFile = new File( zarrPath );

        return new Parameters( resolutions, subdivisions, null, zarrFile, sourceTransform,
                downsamplingMethod, compression, viewSetupNames );
    }

    @Override
    protected void writeFiles(SequenceDescriptionMinimal seq, Map<Integer, ExportMipmapInfo> perSetupExportMipmapInfo,
                              Parameters params, ExportScalePyramid.LoopbackHeuristic loopbackHeuristic,
                              ExportScalePyramid.AfterEachPlane afterEachPlane, int numCellCreatorThreads,
                              ProgressWriter progressWriter, int numTimepoints, int numSetups ) throws IOException, SpimDataException {
        WriteSequenceToN5OmeZarr.writeOmeZarrFile( seq, perSetupExportMipmapInfo,
                params.downsamplingMethod,
                params.compression, params.n5File,
                loopbackHeuristic, afterEachPlane, numCellCreatorThreads,
                new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );

        progressWriter.setProgress( 1.0 );
    }
}
