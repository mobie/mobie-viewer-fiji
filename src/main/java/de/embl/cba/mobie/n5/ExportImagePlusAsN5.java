package de.embl.cba.mobie.n5;

// copy of https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java
// with GUI removed and some refactoring

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.n5.WriteSequenceToN5;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.imagestack.ImageStackImageLoader;
import bdv.img.n5.N5ImageLoader;
import bdv.img.virtualstack.VirtualStackImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import mpicbg.spim.data.SpimDataException;
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
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import static de.embl.cba.mobie.utils.ExportUtils.*;

public class ExportImagePlusAsN5
{



    public void export ( ImagePlus imp, String xmlPath ) {

        if ( !isImageSuitable(imp) ) {
            return;
        }

        FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        FinalDimensions size = getSize( imp );

        // propose reasonable mipmap settings
        final int maxNumElements = 64 * 64 * 64;
        final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps(
                new BasicViewSetup( 0, "", size, voxelSize ),
                maxNumElements );

        // use default parameters
        final N5Parameters params = getDefaultParameters( autoMipmapSettings, xmlPath );
        if ( params == null )
            return;

        export( imp, params );

    }

    public void export ( ImagePlus imp, String subsamplingFactors, String n5ChunkSizes, int compressionChoice, String xmlPath ) {
        final N5Parameters params = getManualParameters( subsamplingFactors, n5ChunkSizes, compressionChoice, xmlPath );

        if ( params != null ) {
            export(imp, params);
        }

    }

    public void export ( ImagePlus imp, N5Parameters params )
    {
        if ( !isImageSuitable(imp) ) {
            return;
        }

        final FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        final FinalDimensions size = getSize( imp );

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

        // create SourceTransform from the images calibration
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        sourceTransform.set( voxelSize.dimension(0), 0, 0, 0, 0,
                voxelSize.dimension(1), 0, 0, 0, 0, voxelSize.dimension(2), 0 );

        // write n5
        final HashMap< Integer, BasicViewSetup > setups = new HashMap<>( numSetups );
        for ( int s = 0; s < numSetups; ++s )
        {
            final BasicViewSetup setup = new BasicViewSetup( s, String.format( "channel %d", s + 1 ), size, voxelSize );
            setup.setAttribute( new Channel( s + 1 ) );
            setups.put( s, setup );
        }
        final ArrayList< TimePoint > timepoints = new ArrayList<>( numTimepoints );
        for ( int t = 0; t < numTimepoints; ++t )
            timepoints.add( new TimePoint( t ) );
        final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );

        Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo;
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
        final LoopbackHeuristic loopbackHeuristic = new LoopbackHeuristic()
        {
            @Override
            public boolean decide( final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
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

        final AfterEachPlane afterEachPlane = new AfterEachPlane()
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
            WriteSequenceToN5.writeN5File( seq, perSetupExportMipmapInfo,
                    params.compression, params.n5File,
                    loopbackHeuristic, afterEachPlane, numCellCreatorThreads,
                    new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );

            // write xml sequence description
            final N5ImageLoader n5Loader = new N5ImageLoader( params.n5File, null );
            final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal( seq, n5Loader );

            final ArrayList< ViewRegistration > registrations = new ArrayList<>();
            for ( int t = 0; t < numTimepoints; ++t )
                for ( int s = 0; s < numSetups; ++s )
                    registrations.add( new ViewRegistration( t, s, sourceTransform ) );

            final File basePath = params.seqFile.getParentFile();
            final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seqh5, new ViewRegistrations( registrations ) );

            new XmlIoSpimDataMinimal().save( spimData, params.seqFile.getAbsolutePath() );
            progressWriter.setProgress( 1.0 );
        }
        catch ( final SpimDataException | IOException e )
        {
            throw new RuntimeException( e );
        }
        progressWriter.out().println( "done" );
    }

    public static class N5Parameters
    {
        final boolean setMipmapManual;

        final int[][] resolutions;

        final int[][] subdivisions;

        final File seqFile;

        final File n5File;

        final Compression compression;

        public N5Parameters(
                final boolean setMipmapManual, final int[][] resolutions, final int[][] subdivisions,
                final File seqFile, final File n5File,
                final Compression compression )
        {
            this.setMipmapManual = setMipmapManual;
            this.resolutions = resolutions;
            this.subdivisions = subdivisions;
            this.seqFile = seqFile;
            this.n5File = n5File;
            this.compression = compression;
        }
    }



    private File getN5File ( String xmlPath ) {
        final String n5Filename = xmlPath.substring( 0, xmlPath.length() - 4 ) + ".n5";
        return new File( n5Filename );
    }

    static String lastSubsampling = "{ {1,1,1} }";

    static String lastChunkSizes = "{ {64,64,64} }";

    static String lastCompressionChoice = "raw (no compression)";

    public N5Parameters getManualParameters( String subsamplingFactors, String n5ChunkSizes, int compressionChoice, String exportPath ) {

        // parse mipmap resolutions and cell sizes
        final int[][] resolutions = PluginHelper.parseResolutionsString( subsamplingFactors );
        final int[][] subdivisions = PluginHelper.parseResolutionsString( n5ChunkSizes );
        if ( !isResolutionsAndSubdivisionsSuitable( resolutions, subdivisions, subsamplingFactors, n5ChunkSizes )) {
            return null;
        }

        final File seqFile = getSeqFileFromPath( exportPath );
        if ( seqFile == null ) {
            return null;
        }

        File n5File = getN5File( exportPath );

        final Compression compression;
        switch ( compressionChoice )
        {
            default:
            case 0: // raw (no compression)
                compression = new RawCompression();
                break;
            case 1: // bzip
                compression = getBzip2Settings();
                break;
            case 2: // gzip
                compression = getGzipSettings();
                break;
            case 3:// lz4
                compression = getLz4Settings();
                break;
            case 4:// xz
                compression = getXzSettings();
                break;
        }
        if ( compression == null )
            return null;

        return new N5Parameters( true, resolutions, subdivisions, seqFile, n5File, compression );
    }

    protected N5Parameters getDefaultParameters ( final ExportMipmapInfo autoMipmapSettings, String exportPath ) {
        final String autoSubsampling = ProposeMipmaps.getArrayString( autoMipmapSettings.getExportResolutions() );
        final String autoChunkSizes = ProposeMipmaps.getArrayString( autoMipmapSettings.getSubdivisions() );

        return getManualParameters( autoSubsampling, autoChunkSizes, 0, exportPath );
    }

    static int lastBzip2BlockSize = BZip2CompressorOutputStream.MAX_BLOCKSIZE;

    protected Bzip2Compression getBzip2Settings()
    {
        while ( true )
        {
            final GenericDialogPlus gd = new GenericDialogPlus( "Bzip2 compression settings" );
            gd.addNumericField(
                    String.format( "block size (%d-%d)",
                            BZip2CompressorOutputStream.MIN_BLOCKSIZE,
                            BZip2CompressorOutputStream.MAX_BLOCKSIZE ),
                    lastBzip2BlockSize, 0 );
            gd.addMessage( "as 100k units" );

            gd.showDialog();
            if ( gd.wasCanceled() )
                return null;

            lastBzip2BlockSize = ( int ) gd.getNextNumber();
            if ( lastBzip2BlockSize < BZip2CompressorOutputStream.MIN_BLOCKSIZE || lastBzip2BlockSize > BZip2CompressorOutputStream.MAX_BLOCKSIZE )
            {
                IJ.showMessage(
                        String.format( "Block size must be in range [%d, %d]",
                                BZip2CompressorOutputStream.MIN_BLOCKSIZE,
                                BZip2CompressorOutputStream.MAX_BLOCKSIZE ) );
                continue;
            }
            return new Bzip2Compression( lastBzip2BlockSize );
        }
    }

    static int lastGzipLevel = 6;

    static boolean lastGzipUseZlib = false;

    protected GzipCompression getGzipSettings()
    {
        while ( true )
        {
            final GenericDialogPlus gd = new GenericDialogPlus( "Gzip compression settings" );
            gd.addNumericField( "level (0-9)", lastGzipLevel, 0 );
            gd.addCheckbox( "use Zlib", lastGzipUseZlib );

            gd.showDialog();
            if ( gd.wasCanceled() )
                return null;

            lastGzipLevel = ( int ) gd.getNextNumber();
            lastGzipUseZlib = gd.getNextBoolean();
            if ( lastGzipLevel < 0 || lastGzipLevel > 9 )
            {
                IJ.showMessage( "Level must be in range [0, 9]" );
                continue;
            }
            return new GzipCompression( lastGzipLevel, lastGzipUseZlib );
        }
    }

    static int lastLz4BlockSize = 1 << 16;

    protected Lz4Compression getLz4Settings()
    {
        final int COMPRESSION_LEVEL_BASE = 10;
        final int MIN_BLOCK_SIZE = 64;
        final int MAX_BLOCK_SIZE = 1 << (COMPRESSION_LEVEL_BASE + 0x0F);

        while ( true )
        {
            final GenericDialogPlus gd = new GenericDialogPlus( "LZ4 compression settings" );
            gd.addNumericField(
                    String.format( "block size (%d-%d)",
                            MIN_BLOCK_SIZE,
                            MAX_BLOCK_SIZE ),
                    lastLz4BlockSize, 0, 8, null );

            gd.showDialog();
            if ( gd.wasCanceled() )
                return null;

            lastLz4BlockSize = ( int ) gd.getNextNumber();
            if ( lastLz4BlockSize < MIN_BLOCK_SIZE || lastLz4BlockSize > MAX_BLOCK_SIZE )
            {
                IJ.showMessage( String.format( "Block size must be in range [%d, %d]",
                        MIN_BLOCK_SIZE,
                        MAX_BLOCK_SIZE ) );
                continue;
            }
            return new Lz4Compression( lastLz4BlockSize );
        }
    }

    static int lastXzLevel = 6;

    protected XzCompression getXzSettings()
    {
        while ( true )
        {
            final GenericDialogPlus gd = new GenericDialogPlus( "XZ compression settings" );
            gd.addNumericField( "level (0-9)", lastXzLevel, 0 );
            gd.addMessage( "LZMA2 preset level" );

            gd.showDialog();
            if ( gd.wasCanceled() )
                return null;

            lastXzLevel = ( int ) gd.getNextNumber();
            if ( lastXzLevel < 0 || lastXzLevel > 9 )
            {
                IJ.showMessage( "Level must be in range [0, 9]" );
                continue;
            }
            return new XzCompression( lastXzLevel );
        }
    }

}
