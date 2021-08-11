package de.embl.cba.mobie.projectcreator.n5;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.imagestack.ImageStackImageLoader;
import bdv.img.n5.N5ImageLoader;
import bdv.img.virtualstack.VirtualStackImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import com.google.gson.annotations.SerializedName;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.Compression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.*;

// splits functionality from https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java
// separating from GUI. Provides general export functions for ImagePlus.

public class WriteImgPlusToN5 {

    protected static class Parameters
    {
        final int[][] resolutions;

        final int[][] subdivisions;

        final File seqFile;

        final File n5File;

        final AffineTransform3D sourceTransform;

        final DownsampleBlock.DownsamplingMethod downsamplingMethod;

        final Compression compression;

        final String[] viewSetupNames;

        public Parameters(
                final int[][] resolutions, final int[][] subdivisions,
                final File seqFile, final File n5File, final AffineTransform3D sourceTransform,
                final DownsampleBlock.DownsamplingMethod downsamplingMethod, final Compression compression )
        {
            this.resolutions = resolutions;
            this.subdivisions = subdivisions;
            this.seqFile = seqFile;
            this.n5File = n5File;
            this.sourceTransform = sourceTransform;
            this.downsamplingMethod = downsamplingMethod;
            this.compression = compression;
            this.viewSetupNames = null;
        }

        public Parameters(
                final int[][] resolutions, final int[][] subdivisions,
                final File seqFile, final File n5File, final AffineTransform3D sourceTransform,
                final DownsampleBlock.DownsamplingMethod downsamplingMethod, final Compression compression,
                final String[] viewSetupNames )
        {
            this.resolutions = resolutions;
            this.subdivisions = subdivisions;
            this.seqFile = seqFile;
            this.n5File = n5File;
            this.sourceTransform = sourceTransform;
            this.downsamplingMethod = downsamplingMethod;
            this.compression = compression;
            this.viewSetupNames = viewSetupNames;
        }
    }

    // export, generating default source transform, and default resolutions / subdivisions
    public void export( ImagePlus imp, String xmlPath, DownsampleBlock.DownsamplingMethod downsamplingMethod,
                        Compression compression ) {
        if ( !isImageSuitable( imp ) ) {
            return;
        }

        FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        final AffineTransform3D sourceTransform = generateSourceTransform( voxelSize );

        Parameters defaultParameters = generateDefaultParameters( imp, xmlPath, sourceTransform, downsamplingMethod,
                compression, null );

        export( imp, defaultParameters );
    }

    // export, generating default resolutions / subdivisions
    public void export(ImagePlus imp, String xmlPath, AffineTransform3D sourceTransform,
                       DownsampleBlock.DownsamplingMethod downsamplingMethod, Compression compression ) {
        if ( !isImageSuitable( imp ) ) {
            return;
        }

        Parameters defaultParameters = generateDefaultParameters( imp, xmlPath, sourceTransform,
                downsamplingMethod, compression, null );

        export( imp, defaultParameters );
    }

    // export, generating default resolutions / subdivisions
    public void export(ImagePlus imp, String xmlPath, AffineTransform3D sourceTransform,
                       DownsampleBlock.DownsamplingMethod downsamplingMethod, Compression compression,
                       String[] viewSetupNames ) {
        if ( !isImageSuitable( imp ) ) {
            return;
        }

        Parameters defaultParameters = generateDefaultParameters( imp, xmlPath, sourceTransform,
                downsamplingMethod, compression, viewSetupNames );

        export( imp, defaultParameters );
    }

    public void export( ImagePlus imp, int[][] resolutions, int[][] subdivisions, String xmlPath,
                        AffineTransform3D sourceTransform, DownsampleBlock.DownsamplingMethod downsamplingMethod,
                        Compression compression ) {
        export( imp, resolutions, subdivisions, xmlPath, sourceTransform, downsamplingMethod, compression, null );
    }

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

        final File n5File = getN5FileFromXmlPath( seqFilename );

        // TODO - check transform and downsampling mode

        Parameters exportParameters = new Parameters( resolutions, subdivisions, seqFile, n5File, sourceTransform,
                downsamplingMethod, compression, viewSetupNames );

        export( imp, exportParameters );
    }

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

        final File n5File = getN5FileFromXmlPath( seqFilename );

        return new Parameters( resolutions, subdivisions, seqFile, n5File, sourceTransform,
                downsamplingMethod, compression, viewSetupNames );
    }

    protected void export( ImagePlus imp, Parameters params ) {

        FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        FinalDimensions size = getSize( imp );

        final ProgressWriter progressWriter = new ProgressWriterIJ();
        progressWriter.out().println( "starting export..." );

        // create ImgLoader wrapping the image
        final TypedBasicImgLoader< ? > imgLoader;
        final Runnable clearCache;
        final boolean isVirtual = imp.getStack() != null && imp.getStack().isVirtual();
        if ( isVirtual )
        {
            final VirtualStackImageLoader< ?, ?, ? > il;
            switch ( imp.getType() )
            {
                case ImagePlus.GRAY8:
                    il = VirtualStackImageLoader.createUnsignedByteInstance( imp );
                    break;
                case ImagePlus.GRAY16:
                    il = VirtualStackImageLoader.createUnsignedShortInstance( imp );
                    break;
                case ImagePlus.GRAY32:
                default:
                    il = VirtualStackImageLoader.createFloatInstance( imp );
                    break;
            }
            imgLoader = il;
            clearCache = il.getCacheControl()::clearCache;
        }
        else
        {
            switch ( imp.getType() )
            {
                case ImagePlus.GRAY8:
                    imgLoader = ImageStackImageLoader.createUnsignedByteInstance( imp );
                    break;
                case ImagePlus.GRAY16:
                    imgLoader = ImageStackImageLoader.createUnsignedShortInstance( imp );
                    break;
                case ImagePlus.GRAY32:
                default:
                    imgLoader = ImageStackImageLoader.createFloatInstance( imp );
                    break;
            }
            clearCache = () -> {};
        }

        final int numTimepoints = imp.getNFrames();
        final int numSetups = imp.getNChannels();

        // write n5
        final HashMap< Integer, BasicViewSetup> setups = new HashMap<>( numSetups );
        if ( params.viewSetupNames != null && params.viewSetupNames.length != numSetups ) {
            throw new RuntimeException( params.viewSetupNames.length + " view setup names were given, " +
                    "but there are " + numSetups + "setups" );
        }
        for ( int s = 0; s < numSetups; ++s )
        {
            final BasicViewSetup setup;
            if ( params.viewSetupNames != null ) {
                setup = new BasicViewSetup(s, params.viewSetupNames[s], size, voxelSize);
            } else {
                setup = new BasicViewSetup(s, String.format("channel %d", s + 1), size, voxelSize);
            }
            setup.setAttribute( new Channel( s + 1 ) );
            setups.put( s, setup );
        }
        final ArrayList<TimePoint> timepoints = new ArrayList<>( numTimepoints );
        for ( int t = 0; t < numTimepoints; ++t )
            timepoints.add( new TimePoint( t ) );
        final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );

        Map< Integer, ExportMipmapInfo> perSetupExportMipmapInfo;
        perSetupExportMipmapInfo = new HashMap<>();
        final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( params.resolutions, params.subdivisions );
        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
            perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );

        // LoopBackHeuristic:
        // - If saving more than 8x on pixel reads use the loopback image over
        //   original image
        // - For virtual stacks also consider the cache size that would be
        //   required for all original planes contributing to a "plane of
        //   blocks" at the current level. If this is more than 1/4 of
        //   available memory, use the loopback image.
        final long planeSizeInBytes = imp.getWidth() * imp.getHeight() * imp.getBytesPerPixel();
        final long ijMaxMemory = IJ.maxMemory();
        final int numCellCreatorThreads = Math.max( 1, PluginHelper.numThreads() - 1 );
        final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic = new ExportScalePyramid.LoopbackHeuristic()
        {
            @Override
            public boolean decide(final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
            {
                if ( previousLevel < 0 )
                    return false;

                if ( Intervals.numElements( factorsToOriginalImg ) / Intervals.numElements( factorsToPreviousLevel ) >= 8 )
                    return true;

                if ( isVirtual )
                {
                    final long requiredCacheSize = planeSizeInBytes * factorsToOriginalImg[ 2 ] * chunkSize[ 2 ];
                    if ( requiredCacheSize > ijMaxMemory / 4 )
                        return true;
                }

                return false;
            }
        };

        final ExportScalePyramid.AfterEachPlane afterEachPlane = new ExportScalePyramid.AfterEachPlane()
        {
            @Override
            public void afterEachPlane( final boolean usedLoopBack )
            {
                if ( !usedLoopBack && isVirtual )
                {
                    final long free = Runtime.getRuntime().freeMemory();
                    final long total = Runtime.getRuntime().totalMemory();
                    final long max = Runtime.getRuntime().maxMemory();
                    final long actuallyFree = max - total + free;

                    if ( actuallyFree < max / 2 )
                        clearCache.run();
                }
            }

        };

        try
        {
            writeFiles( seq, perSetupExportMipmapInfo, params, loopbackHeuristic, afterEachPlane, numCellCreatorThreads,
                    progressWriter, numTimepoints, numSetups );
        }
        catch ( final SpimDataException | IOException e )
        {
            throw new RuntimeException( e );
        }
        progressWriter.out().println( "done" );
    }

    protected void writeFiles(SequenceDescriptionMinimal seq, Map<Integer, ExportMipmapInfo> perSetupExportMipmapInfo,
                              Parameters params, ExportScalePyramid.LoopbackHeuristic loopbackHeuristic,
                              ExportScalePyramid.AfterEachPlane afterEachPlane, int numCellCreatorThreads,
                              ProgressWriter progressWriter, int numTimepoints, int numSetups ) throws IOException, SpimDataException {
        WriteSequenceToN5.writeN5File( seq, perSetupExportMipmapInfo,
                params.downsamplingMethod,
                params.compression, params.n5File,
                loopbackHeuristic, afterEachPlane, numCellCreatorThreads,
                new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );

        // write xml sequence description
        final N5ImageLoader n5Loader = new N5ImageLoader( params.n5File, null );
        final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal( seq, n5Loader );

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