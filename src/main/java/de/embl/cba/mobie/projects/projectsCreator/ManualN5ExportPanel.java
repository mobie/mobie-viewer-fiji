package de.embl.cba.mobie.projects.projectsCreator;

import bdv.ij.util.PluginHelper;
import de.embl.cba.mobie.n5.DownsampleBlock;
import de.embl.cba.mobie.n5.WriteImgPlusToN5;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.janelia.saalfeldlab.n5.*;

// based on https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java
// removing export path, that shouldn't be set manually

public class ManualN5ExportPanel {

    ImagePlus imp;
    String xmlPath;
    AffineTransform3D sourceTransform;
    DownsampleBlock.DownsamplingMethod downsamplingMethod;

    static String lastSubsampling = "{ {1,1,1} }";
    static String lastChunkSizes = "{ {64,64,64} }";
    static int lastCompressionChoice = 0;
    static boolean lastCompressionDefaultSettings = true;

    public ManualN5ExportPanel ( ImagePlus imp, String xmlPath, AffineTransform3D sourceTransform,
                                 DownsampleBlock.DownsamplingMethod downsamplingMethod ) {
        this.imp = imp;
        this.xmlPath = xmlPath;
        this.sourceTransform = sourceTransform;
        this.downsamplingMethod = downsamplingMethod;
    }

    public void getManualExportParameters() {

        final GenericDialog manualSettings = new GenericDialog( "Manual Settings for BigDataViewer XML/N5" );

        // same settings as https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java#L345
        // but hiding settings like e.g. export location that shouldn't be set manually

        manualSettings.addStringField( "Subsampling_factors", lastSubsampling, 25 );
        manualSettings.addStringField( "N5_chunk_sizes", lastChunkSizes, 25 );
        final String[] compressionChoices = new String[] { "raw (no compression)", "bzip", "gzip", "lz4", "xz" };
        manualSettings.addChoice( "compression", compressionChoices, compressionChoices[ lastCompressionChoice ] );
        manualSettings.addCheckbox( "use default settings for compression", lastCompressionDefaultSettings );

        manualSettings.showDialog();

        if ( !manualSettings.wasCanceled() ) {
            lastSubsampling = manualSettings.getNextString();
            lastChunkSizes = manualSettings.getNextString();
            lastCompressionChoice = manualSettings.getNextChoiceIndex();
            lastCompressionDefaultSettings = manualSettings.getNextBoolean();

            parseInputAndWriteToN5();
        }

    }

    private void parseInputAndWriteToN5 () {
        // parse mipmap resolutions and cell sizes
        final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
        final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );

        final Compression compression;
        switch ( lastCompressionChoice )
        {
            default:
            case 0: // raw (no compression)
                compression = new RawCompression();
                break;
            case 1: // bzip
                compression = lastCompressionDefaultSettings
                        ? new Bzip2Compression()
                        : getBzip2Settings();
                break;
            case 2: // gzip
                compression = lastCompressionDefaultSettings
                        ? new GzipCompression()
                        : getGzipSettings();
                break;
            case 3:// lz4
                compression = lastCompressionDefaultSettings
                        ? new Lz4Compression()
                        : getLz4Settings();
                break;
            case 4:// xz" };
                compression = lastCompressionDefaultSettings
                        ? new XzCompression()
                        : getXzSettings();
                break;
        }
        if ( compression == null )
            return;

        new WriteImgPlusToN5().export( imp, resolutions, subdivisions, xmlPath, sourceTransform,
                downsamplingMethod, compression );
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
