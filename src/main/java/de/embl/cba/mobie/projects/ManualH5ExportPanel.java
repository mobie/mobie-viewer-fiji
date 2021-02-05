package de.embl.cba.mobie.projects;

import bdv.ij.export.imgloader.ImagePlusImgLoader;
import bdv.ij.util.PluginHelper;
import de.embl.cba.mobie.h5.WriteImgPlusToH5;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;

public class ManualH5ExportPanel {

    ImagePlus imp;
    String xmlPath;
    AffineTransform3D sourceTransform;
    String downsamplingMode;

    static String lastSubsampling = "{ {1,1,1} }";
    static String lastChunkSizes = "{ {64,64,64} }";
    static int lastMinMaxChoice = 2;
    static double lastMin = 0;
    static double lastMax = 65535;
    static boolean lastSplit = false;
    static int lastTimepointsPerPartition = 0;
    static int lastSetupsPerPartition = 0;
    static boolean lastDeflate = true;
    static int lastDownsamplingModeChoice = 0;

    public ManualH5ExportPanel( ImagePlus imp, String xmlPath, AffineTransform3D sourceTransform,
                               String downsamplingMode ) {
        this.imp = imp;
        this.xmlPath = xmlPath;
        this.sourceTransform = sourceTransform;
        this.downsamplingMode = downsamplingMode;
    }

    public void getManualExportParameters() {

        final GenericDialog manualSettings = new GenericDialog( "Manual Settings for BigDataViewer XML/H5" );

        // same settings as https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusPlugIn.java#L357
        // but hiding settings like e.g. export location that shouldn't be set manually

        manualSettings.addStringField( "Subsampling_factors", lastSubsampling, 25 );
        manualSettings.addStringField( "Hdf5_chunk_sizes", lastChunkSizes, 25 );

        manualSettings.addMessage( "" );
        final String[] minMaxChoices = new String[] { "Use ImageJ's current min/max setting", "Compute min/max of the (hyper-)stack", "Use values specified below" };
        manualSettings.addChoice( "Value_range", minMaxChoices, minMaxChoices[ lastMinMaxChoice ] );
        manualSettings.addNumericField( "Min", lastMin, 0 );
        manualSettings.addNumericField( "Max", lastMax, 0 );

        manualSettings.addMessage( "" );
        manualSettings.addCheckbox( "split_hdf5", lastSplit );
        manualSettings.addNumericField( "timepoints_per_partition", lastTimepointsPerPartition, 0, 25, "" );
        manualSettings.addNumericField( "setups_per_partition", lastSetupsPerPartition, 0, 25, "" );

        manualSettings.addMessage( "" );
        manualSettings.addCheckbox( "use_deflate_compression", lastDeflate );

        manualSettings.showDialog();

        if ( !manualSettings.wasCanceled() ) {
            lastSubsampling = manualSettings.getNextString();
            lastChunkSizes = manualSettings.getNextString();
            lastMinMaxChoice = manualSettings.getNextChoiceIndex();
            lastMin = manualSettings.getNextNumber();
            lastMax = manualSettings.getNextNumber();
            lastSplit = manualSettings.getNextBoolean();
            lastTimepointsPerPartition = (int) manualSettings.getNextNumber();
            lastSetupsPerPartition = (int) manualSettings.getNextNumber();
            lastDeflate = manualSettings.getNextBoolean();

            parseInputAndWriteToH5();
        }
    }

    private void parseInputAndWriteToH5() {
        // parse mipmap resolutions and cell sizes
        final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
        final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );

        final ImagePlusImgLoader.MinMaxOption minMaxOption;
        if ( lastMinMaxChoice == 0 )
            minMaxOption = ImagePlusImgLoader.MinMaxOption.TAKE_FROM_IMAGEPROCESSOR;
        else if ( lastMinMaxChoice == 1 )
            minMaxOption = ImagePlusImgLoader.MinMaxOption.COMPUTE;
        else
            minMaxOption = ImagePlusImgLoader.MinMaxOption.SET;

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

        new WriteImgPlusToH5().export( imp, resolutions, subdivisions, xmlPath, minMaxOption, lastMin, lastMax,
                lastDeflate, lastSplit, lastTimepointsPerPartition, lastSetupsPerPartition, sourceTransform, downsamplingMode );
    }
}
