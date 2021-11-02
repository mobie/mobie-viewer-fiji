package org.embl.mobie.viewer.projectcreator.ui;

import bdv.ij.util.PluginHelper;
import org.embl.mobie.io.ome.zarr.writers.imgplus.WriteImgPlusToN5BdvOmeZarr;
import org.embl.mobie.io.ome.zarr.writers.imgplus.WriteImgPlusToN5OmeZarr;
import org.embl.mobie.io.util.DownsampleBlock;
import org.embl.mobie.io.util.writers.WriteImgPlusToN5;
import org.embl.mobie.viewer.source.ImageDataFormat;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.janelia.saalfeldlab.n5.*;

// based on https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java
// removing export path, that shouldn't be set manually

public class ManualExportPanel {

    ImagePlus imp;
    String filePath;
    AffineTransform3D sourceTransform;
    DownsampleBlock.DownsamplingMethod downsamplingMethod;
    String imageName;
    ImageDataFormat imageDataFormat;

    static String lastSubsampling = "{ {1,1,1} }";
    static String lastChunkSizes = "{ {64,64,64} }";
    static int lastCompressionChoice = 0;
    static boolean lastCompressionDefaultSettings = true;

    public ManualExportPanel(ImagePlus imp, String filePath, AffineTransform3D sourceTransform,
                             DownsampleBlock.DownsamplingMethod downsamplingMethod, String imageName,
                             ImageDataFormat imageDataFormat ) {
        this.imp = imp;
        this.filePath = filePath;
        this.sourceTransform = sourceTransform;
        this.downsamplingMethod = downsamplingMethod;
        this.imageName = imageName;
        this.imageDataFormat = imageDataFormat;
    }

    public void getManualExportParameters() {

        final GenericDialog manualSettings = new GenericDialog( "Manual Settings for " +
                imageDataFormat.toString() );

        // same settings as https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java#L345
        // but hiding settings like e.g. export location that shouldn't be set manually

        manualSettings.addStringField( "Subsampling_factors", lastSubsampling, 25 );
        manualSettings.addStringField( "chunk_sizes", lastChunkSizes, 25 );

        // TODO - the ome-zarr code doesn't seem to support all the compression options at the moment. Would need to
        // look into this more. For now, don't show compression options for ome-zarr
        if ( imageDataFormat == ImageDataFormat.BdvN5 ) {
            final String[] compressionChoices = new String[]{"raw (no compression)", "bzip", "gzip", "lz4", "xz"};
            manualSettings.addChoice("compression", compressionChoices, compressionChoices[lastCompressionChoice]);
            manualSettings.addCheckbox("use default settings for compression", lastCompressionDefaultSettings);
        }

        manualSettings.showDialog();

        if ( !manualSettings.wasCanceled() ) {
            lastSubsampling = manualSettings.getNextString();
            lastChunkSizes = manualSettings.getNextString();
            if ( imageDataFormat == ImageDataFormat.BdvN5 ) {
                lastCompressionChoice = manualSettings.getNextChoiceIndex();
                lastCompressionDefaultSettings = manualSettings.getNextBoolean();
            } else {
                lastCompressionChoice = 2;
                lastCompressionDefaultSettings = true;
            }

            parseInputAndWriteImage();
        }

    }

    private void parseInputAndWriteImage() {
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

        writeImage( resolutions, subdivisions, compression );
    }

    private void writeImage( int[][] resolutions, int[][] subdivisions, Compression compression ) {
        switch( imageDataFormat ) {
            case BdvN5:
                new WriteImgPlusToN5().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            case BdvOmeZarr:
                new WriteImgPlusToN5BdvOmeZarr().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            case OmeZarr:
                new WriteImgPlusToN5OmeZarr().export(imp, resolutions, subdivisions, filePath, sourceTransform,
                        downsamplingMethod, compression, new String[]{imageName});
                break;

            default:
                throw new UnsupportedOperationException();
        }
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
