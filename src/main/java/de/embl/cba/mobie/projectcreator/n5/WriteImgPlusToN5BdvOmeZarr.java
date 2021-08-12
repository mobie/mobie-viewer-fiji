package de.embl.cba.mobie.projectcreator.n5;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.mobie.n5.zarr.N5OMEZarrImageLoader;
import de.embl.cba.mobie.n5.zarr.N5OmeZarrReader;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.Compression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.*;
import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.getOmeZarrFileFromXmlPath;

public class WriteImgPlusToN5BdvOmeZarr extends WriteImgPlusToN5 {

    @Override
    public void export( ImagePlus imp, int[][] resolutions, int[][] subdivisions, String xmlPath,
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

        String seqFilename = xmlPath;
        if ( !seqFilename.endsWith( ".xml" ) )
            seqFilename += ".xml";
        final File seqFile = getSeqFileFromPath( seqFilename );
        if ( seqFile == null ) {
            return;
        }

        final File zarrFile = getOmeZarrFileFromXmlPath( seqFilename );

        // TODO - check transform and downsampling mode

        Parameters exportParameters = new Parameters( resolutions, subdivisions, seqFile, zarrFile, sourceTransform,
                downsamplingMethod, compression, viewSetupNames );

        export( imp, exportParameters );
    }

    // TODO - split some of this into common functions
    @Override
    protected Parameters generateDefaultParameters(ImagePlus imp, String xmlPath, AffineTransform3D sourceTransform,
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

        String seqFilename = xmlPath;
        if ( !seqFilename.endsWith( ".xml" ) )
            seqFilename += ".xml";
        final File seqFile = getSeqFileFromPath( seqFilename );
        if ( seqFile == null ) {
            return null;
        }

        final File zarrFile = getOmeZarrFileFromXmlPath( seqFilename );

        return new Parameters( resolutions, subdivisions, seqFile, zarrFile, sourceTransform,
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

        // write xml sequence description
        final N5OMEZarrImageLoader zarrLoader = new N5OMEZarrImageLoader(
                new N5OmeZarrReader( params.n5File.getAbsolutePath() ), seq );
        final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal( seq, zarrLoader );

        final ArrayList<ViewRegistration> registrations = new ArrayList<>();
        for ( int t = 0; t < numTimepoints; ++t )
            for ( int s = 0; s < numSetups; ++s )
                registrations.add( new ViewRegistration( t, s, params.sourceTransform ) );

        final File basePath = params.seqFile.getParentFile();
        final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seqh5, new ViewRegistrations( registrations ) );

        new XmlIoSpimDataMinimal().save( spimData, params.seqFile.getAbsolutePath() );

        progressWriter.setProgress( 1.0 );
    }
}
