package de.embl.cba.mobie.h5;

import bdv.export.*;
import bdv.ij.export.imgloader.ImagePlusImgLoader;
import bdv.ij.util.PluginHelper;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.awt.event.ItemEvent;

import static de.embl.cba.mobie.utils.ExportUtils.*;

/**
 * ImageJ plugin to export the current image to xml/hdf5.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
@Plugin(type = Command.class,
        menuPath = "Plugins>BigDataViewer>Export Current Image as XML/HDF5 (Experimental)")
public class ExportImagePlusPlugIn implements Command
{
    public static void main( final String[] args )
    {
        new ImageJ();
        IJ.run("Confocal Series (2.2MB)");
        new bdv.ij.ExportImagePlusPlugIn().run();
    }

    @Override
    public void run()
    {
        if ( ij.Prefs.setIJMenuBar )
            System.setProperty( "apple.laf.useScreenMenuBar", "true" );

        // get the current image
        final ImagePlus imp = WindowManager.getCurrentImage();

        // make sure there is one
        if ( imp == null )
        {
            IJ.showMessage( "Please open an image first." );
            return;
        }

        if ( !isImageSuitable( imp ) ) {
            return;
        }

        // get calibration and image size
        FinalVoxelDimensions voxelSize = getVoxelSize( imp );
        FinalDimensions size = getSize( imp );

        // propose reasonable mipmap settings
        final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps( new BasicViewSetup( 0, "", size, voxelSize ) );

        // show dialog to get output paths, resolutions, subdivisions, min-max option
        getParameters( imp, autoMipmapSettings);
    }

    static boolean lastSetMipmapManual = false;

    static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,2}";

    static String lastChunkSizes = "{32,32,4}, {16,16,8}, {8,8,8}";

    static int lastMinMaxChoice = 2;

    static double lastMin = 0;

    static double lastMax = 65535;

    static boolean lastSplit = false;

    static int lastTimepointsPerPartition = 0;

    static int lastSetupsPerPartition = 0;

    static boolean lastDeflate = true;

    static int lastDownsamplingModeChoice = 0;

    static String lastExportPath = "./export.xml";

    protected void getParameters( ImagePlus imp, final ExportMipmapInfo autoMipmapSettings  )
    {
        if ( lastMinMaxChoice == 0 ) // use ImageJs...
        {
            lastMin = imp.getDisplayRangeMin();
            lastMax = imp.getDisplayRangeMax();
        }

        while ( true )
        {
            final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer" );

            gd.addCheckbox( "manual_mipmap_setup", lastSetMipmapManual );
            final Checkbox cManualMipmap = ( Checkbox ) gd.getCheckboxes().lastElement();
            gd.addStringField( "Subsampling_factors", lastSubsampling, 25 );
            final TextField tfSubsampling = ( TextField ) gd.getStringFields().lastElement();
            gd.addStringField( "Hdf5_chunk_sizes", lastChunkSizes, 25 );
            final TextField tfChunkSizes = ( TextField ) gd.getStringFields().lastElement();

            gd.addMessage( "" );
            final String[] minMaxChoices = new String[] { "Use ImageJ's current min/max setting", "Compute min/max of the (hyper-)stack", "Use values specified below" };
            gd.addChoice( "Value_range", minMaxChoices, minMaxChoices[ lastMinMaxChoice ] );
            final Choice cMinMaxChoices = (Choice) gd.getChoices().lastElement();
            gd.addNumericField( "Min", lastMin, 0 );
            final TextField tfMin = (TextField) gd.getNumericFields().lastElement();
            gd.addNumericField( "Max", lastMax, 0 );
            final TextField tfMax = (TextField) gd.getNumericFields().lastElement();

            gd.addMessage( "" );
            gd.addCheckbox( "split_hdf5", lastSplit );
            final Checkbox cSplit = ( Checkbox ) gd.getCheckboxes().lastElement();
            gd.addNumericField( "timepoints_per_partition", lastTimepointsPerPartition, 0, 25, "" );
            final TextField tfSplitTimepoints = ( TextField ) gd.getNumericFields().lastElement();
            gd.addNumericField( "setups_per_partition", lastSetupsPerPartition, 0, 25, "" );
            final TextField tfSplitSetups = ( TextField ) gd.getNumericFields().lastElement();

            gd.addMessage( "" );
            gd.addCheckbox( "use_deflate_compression", lastDeflate );

            gd.addMessage( "" );
            gd.addStringField( "Affine Transform", generateDefaultAffine( imp ), 25);
            final String[] downsamplingModeChoices = new String[] { "average", "nearest neighbour" };
            gd.addChoice( "Downsampling Mode", downsamplingModeChoices, downsamplingModeChoices[ lastDownsamplingModeChoice ] );

            gd.addMessage( "" );
            PluginHelper.addSaveAsFileField( gd, "Export_path", lastExportPath, 25 );

//			gd.addMessage( "" );
//			gd.addMessage( "This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n" );
//			Bead_Registration.addHyperLinkListener( ( MultiLineLabel ) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de" );

            final String autoSubsampling = ProposeMipmaps.getArrayString( autoMipmapSettings.getExportResolutions() );
            final String autoChunkSizes = ProposeMipmaps.getArrayString( autoMipmapSettings.getSubdivisions() );
            gd.addDialogListener( new DialogListener()
            {
                @Override
                public boolean dialogItemChanged(final GenericDialog dialog, final AWTEvent e )
                {
                    gd.getNextBoolean();
                    gd.getNextString();
                    gd.getNextString();
                    gd.getNextChoiceIndex();
                    gd.getNextNumber();
                    gd.getNextNumber();
                    gd.getNextBoolean();
                    gd.getNextNumber();
                    gd.getNextNumber();
                    gd.getNextBoolean();
                    gd.getNextString();
                    gd.getNextChoiceIndex();
                    gd.getNextString();
                    if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cMinMaxChoices )
                    {
                        final boolean enable = cMinMaxChoices.getSelectedIndex() == 2;
                        tfMin.setEnabled( enable );
                        tfMax.setEnabled( enable );
                    }
                    else if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cManualMipmap )
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
                    else if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cSplit )
                    {
                        final boolean split = cSplit.getState();
                        tfSplitTimepoints.setEnabled( split );
                        tfSplitSetups.setEnabled( split );
                    }
                    return true;
                }
            } );

            final boolean enable = lastMinMaxChoice == 2;
            tfMin.setEnabled( enable );
            tfMax.setEnabled( enable );

            tfSubsampling.setEnabled( lastSetMipmapManual );
            tfChunkSizes.setEnabled( lastSetMipmapManual );
            if ( !lastSetMipmapManual )
            {
                tfSubsampling.setText( autoSubsampling );
                tfChunkSizes.setText( autoChunkSizes );
            }

            tfSplitTimepoints.setEnabled( lastSplit );
            tfSplitSetups.setEnabled( lastSplit );

            gd.showDialog();
            if ( gd.wasCanceled() )
                return;

            lastSetMipmapManual = gd.getNextBoolean();
            lastSubsampling = gd.getNextString();
            lastChunkSizes = gd.getNextString();
            lastMinMaxChoice = gd.getNextChoiceIndex();
            lastMin = gd.getNextNumber();
            lastMax = gd.getNextNumber();
            lastSplit = gd.getNextBoolean();
            lastTimepointsPerPartition = ( int ) gd.getNextNumber();
            lastSetupsPerPartition = ( int ) gd.getNextNumber();
            lastDeflate = gd.getNextBoolean();
            // don't store this affine, want to recalculate for each image
            String sourceTransformString = gd.getNextString();
            lastDownsamplingModeChoice = gd.getNextChoiceIndex();
            lastExportPath = gd.getNextString();

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

            AffineTransform3D sourceTransform = parseAffineString( sourceTransformString );
            if ( sourceTransform == null ) {
                return;
            }

            new WriteImgPlusToH5().export( imp, resolutions, subdivisions, lastExportPath, minMaxOption, lastMin, lastMax,
            lastDeflate, lastSplit, lastTimepointsPerPartition, lastSetupsPerPartition, sourceTransform, downsamplingMode );
            return;

        }
    }
}

