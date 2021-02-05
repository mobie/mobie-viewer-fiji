package de.embl.cba.mobie.n5;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProposeMipmaps;
import bdv.ij.util.PluginHelper;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
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

/**
 * ImageJ plugin to export the current image to xml/n5.
 *
 * @author Tobias Pietzsch
 */
@Plugin(type = Command.class,
        menuPath = "Plugins>BigDataViewer>Export Current Image as XML/N5 (experimental)")
public class ExportImagePlusAsN5PlugIn implements Command
{
    public static void main( final String[] args )
    {
        new ImageJ();
        final ImagePlus imp = IJ.openImage( "/Users/pietzsch/workspace/data/confocal-series.tif" );
        imp.show();
        new bdv.ij.ExportImagePlusAsN5PlugIn().run();
    }

    @Override
    public void run() {
        if (ij.Prefs.setIJMenuBar)
            System.setProperty("apple.laf.useScreenMenuBar", "true");

        // get the current image
        final ImagePlus imp = WindowManager.getCurrentImage();

        // make sure there is one
        if (imp == null) {
            IJ.showMessage("Please open an image first.");
            return;
        }

        if ( !isImageSuitable( imp ) ) {
            return;
        }

        FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        FinalDimensions size = getSize( imp );

        // propose reasonable mipmap settings
        final int maxNumElements = 64 * 64 * 64;
        final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps(
                new BasicViewSetup(0, "", size, voxelSize),
                maxNumElements);

        // show dialog to get output paths, resolutions, subdivisions, min-max option
        getParameters( imp, autoMipmapSettings);
    }

    static boolean lastSetMipmapManual = false;

    static String lastSubsampling = "";

    static String lastChunkSizes = "";

    static int lastCompressionChoice = 0;

    static boolean lastCompressionDefaultSettings = true;

    static int lastDownsamplingModeChoice = 0;

    static String lastExportPath = "./export.xml";

    protected void getParameters( ImagePlus imp, final ExportMipmapInfo autoMipmapSettings  )
    {
        while ( true )
        {
            final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer as XML/N5" );

            gd.addCheckbox( "manual_mipmap_setup", lastSetMipmapManual );
            final Checkbox cManualMipmap = ( Checkbox ) gd.getCheckboxes().lastElement();
            gd.addStringField( "Subsampling_factors", lastSubsampling, 25 );
            final TextField tfSubsampling = ( TextField ) gd.getStringFields().lastElement();
            gd.addStringField( "N5_chunk_sizes", lastChunkSizes, 25 );
            final TextField tfChunkSizes = ( TextField ) gd.getStringFields().lastElement();

            gd.addMessage( "" );
            final String[] compressionChoices = new String[] { "raw (no compression)", "bzip", "gzip", "lz4", "xz" };
            gd.addChoice( "compression", compressionChoices, compressionChoices[ lastCompressionChoice ] );
            gd.addCheckbox( "default settings", lastCompressionDefaultSettings );

            gd.addMessage( "" );
            gd.addStringField( "Affine Transform", generateDefaultAffine( imp ), 25);
            final String[] downsamplingModeChoices = new String[] { "average", "nearest neighbour" };
            gd.addChoice( "Downsampling Mode", downsamplingModeChoices, downsamplingModeChoices[ lastDownsamplingModeChoice ] );

            gd.addMessage( "" );
            PluginHelper.addSaveAsFileField( gd, "Export_path", lastExportPath, 25 );

            final String autoSubsampling = ProposeMipmaps.getArrayString( autoMipmapSettings.getExportResolutions() );
            final String autoChunkSizes = ProposeMipmaps.getArrayString( autoMipmapSettings.getSubdivisions() );
            gd.addDialogListener( ( dialog, e ) -> {
                gd.getNextBoolean();
                gd.getNextString();
                gd.getNextString();
                gd.getNextChoiceIndex();
                gd.getNextBoolean();
                gd.getNextString();
                gd.getNextChoiceIndex();
                gd.getNextString();
                if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cManualMipmap )
                {
                    final boolean useManual = cManualMipmap.getState();
                    tfSubsampling.setEnabled( useManual );
                    tfChunkSizes.setEnabled( useManual );
                    if ( !useManual )
                    {
                        tfSubsampling.setText( autoSubsampling );
                        tfChunkSizes.setText( autoChunkSizes );
                    }
                }
                return true;
            } );

            tfSubsampling.setEnabled( lastSetMipmapManual );
            tfChunkSizes.setEnabled( lastSetMipmapManual );
            if ( !lastSetMipmapManual )
            {
                tfSubsampling.setText( autoSubsampling );
                tfChunkSizes.setText( autoChunkSizes );
            }

            gd.showDialog();
            if ( gd.wasCanceled() )
                return;

            lastSetMipmapManual = gd.getNextBoolean();
            lastSubsampling = gd.getNextString();
            lastChunkSizes = gd.getNextString();
            lastCompressionChoice = gd.getNextChoiceIndex();
            lastCompressionDefaultSettings = gd.getNextBoolean();
            // don't store this affine, want to recalculate for each image
            String sourceTransformString = gd.getNextString();
            lastDownsamplingModeChoice = gd.getNextChoiceIndex();
            lastExportPath = gd.getNextString();

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

            String downsamplingMode;
            switch ( lastDownsamplingModeChoice ) {
                default:
                case 0: // average
                    downsamplingMode = "average";
                    break;
                case 1: // nearest neighbour
                    downsamplingMode = "nearest neighbour";
                    break;
            }

            AffineTransform3D sourceTransform = parseAffineString( sourceTransformString );
            if ( sourceTransform == null ) {
                return;
            }

            new WriteImgPlusToN5().export( imp, resolutions, subdivisions, lastExportPath, sourceTransform,
                    downsamplingMode, compression );
            return;
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

