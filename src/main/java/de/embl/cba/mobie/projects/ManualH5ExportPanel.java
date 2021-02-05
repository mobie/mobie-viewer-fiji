package de.embl.cba.mobie.projects;

import bdv.ij.util.PluginHelper;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.Compression;

public class ManualH5ExportPanel {

    ImagePlus imp;
    String xmlPath;
    AffineTransform3D sourceTransform;
    String downsamplingMode;

    static String lastSubsampling = "{ {1,1,1} }";
    static String lastChunkSizes = "{ {64,64,64} }";
    static int lastCompressionChoice = 0;
    static boolean lastCompressionDefaultSettings = true;
    static int lastDownsamplingModeChoice = 0;

    public ManualH5ExportPanel(ImagePlus imp, String xmlPath, AffineTransform3D sourceTransform,
                               String downsamplingMode) {
        this.imp = imp;
        this.xmlPath = xmlPath;
        this.sourceTransform = sourceTransform;
        this.downsamplingMode = downsamplingMode;
    }

    public void getManualExportParameters() {

        final GenericDialog manualSettings = new GenericDialog("Manual Settings for BigDataViewer XML/N5");

        // same settings as https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java#L345
        // but hiding settings like e.g. export location that shouldn't be set manually

        manualSettings.addStringField("Subsampling_factors", lastSubsampling, 25);
        manualSettings.addStringField("N5_chunk_sizes", lastChunkSizes, 25);
        final String[] compressionChoices = new String[]{"raw (no compression)", "bzip", "gzip", "lz4", "xz"};
        manualSettings.addChoice("compression", compressionChoices, compressionChoices[lastCompressionChoice]);
        manualSettings.addCheckbox("use default settings for compression", lastCompressionDefaultSettings);

        manualSettings.showDialog();

        if (!manualSettings.wasCanceled()) {
            lastSubsampling = manualSettings.getNextString();
            lastChunkSizes = manualSettings.getNextString();
            lastCompressionChoice = manualSettings.getNextChoiceIndex();
            lastCompressionDefaultSettings = manualSettings.getNextBoolean();

            parseInputAndWriteToH5();
        }

    }

    private void parseInputAndWriteToH5() {
        // parse mipmap resolutions and cell sizes
        final int[][] resolutions = PluginHelper.parseResolutionsString(lastSubsampling);
        final int[][] subdivisions = PluginHelper.parseResolutionsString(lastChunkSizes);

        final Compression compression;
        switch (lastCompressionChoice) {
            default:
            case 0: // raw (no compression)
        }
    }
}
