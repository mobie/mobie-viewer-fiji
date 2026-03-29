package examples;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.context.ScreenShotStackMakerCommand;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;
import org.embl.mobie.lib.transform.viewer.ViewerTransformChanger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

// Groovy port: /Users/tischer/Documents/mobie-viewer-fiji/scripts/screenshotAndNumericAnnotationDisplay.groovy
public class CreatePlatybrowserScreenShotStack
{
    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        // Launch MoBIE
        final MoBIE moBIE = new MoBIE( "https://github.com/mobie/platybrowser-project" );

        // Obtain a handle on BigDataViewer
        final BdvHandle bdvHandle = moBIE.getViewManager().getSliceViewer().getBdvHandle();

        // To obtain such viewer transforms:
        // Right click in BDV and select "Log Current Location"
        // In the ImageJ Log window it will print: {"normalizedAffine":[0.05152857716346159,0.0,0.0,-5.751562308992142,0.0,0.05152857716346159,0.0,-7.918098346939994,0.0,0.0,0.05152857716346159,-5.541408380633763],"timepoint":0}""";
        // Note that you will need to "escape" the inner quotation marks, i.e. replace " with \"
        ViewerTransform viewerTransform = ViewerTransform.toViewerTransform( "{\"normalizedAffine\":[0.05152857716346159,0.0,0.0,-5.751562308992142,0.0,0.05152857716346159,0.0,-7.918098346939994,0.0,0.0,0.05152857716346159,-5.541408380633763],\"timepoint\":0}" );
        ViewerTransformChanger.apply( bdvHandle, viewerTransform );

        // Show ProSPr signal for a muscle gene
        moBIE.getViewManager().show( "mhcl4" );

        // Demonstrate how to create a screenshot and save it
        //
        ScreenShotStackMakerCommand command = new ScreenShotStackMakerCommand();
        command.bdvHandle = bdvHandle;
        command.numSlices = 5;
        command.targetSamplingInXY = 0.2; // micrometer
        command.targetSamplingInZ = 0.5; // micrometer
        command.showImages = false; // don't show, as we will save to image to disk instead
        command.run();

        // Get the screenshot and save it
        ImagePlus rgbImg = command.getRgbImg();
        IJ.save( rgbImg, "/Users/tischer/Downloads/platy-screenshot-stack.tif");


        // Create a new numeric annotation image display
        //

        // Add cell segmentation
        moBIE.getViewManager().show( "cells" );

        // and hide it from the view
        SourceAndConverter< ? > cellsSource = getSource( bdvHandle, "cells" );
        bdvHandle.getViewerPanel().state().setSourceActive( cellsSource, false );

        // Create a new numeric annotation image display...
        Map< String, SegmentationDisplay > displays = moBIE.getViewManager().getCurrentSegmentationDisplays();
        String name = displays.get( "cells" ).getTableView().createNumericAnnotationDisplay( "anchor_x" );

        // ...and change its display settings
        SourceAndConverter< ? > source = getSource( bdvHandle, name );
        // color
        ARGBType red = new ARGBType( ARGBType.rgba( 255, 0, 0, 255 ) );
        ConverterSetup converterSetup = bdvHandle.getConverterSetups().getConverterSetup( source );
        converterSetup.setColor( red );
        // contrast
        converterSetup.setDisplayRange( 100, 200 );
        bdvHandle.getViewerPanel().requestRepaint();
    }

    private static @NotNull SourceAndConverter< ? > getSource( BdvHandle bdvHandle, final String name )
    {
        return bdvHandle.getViewerPanel().state().getSources().stream()
                .filter( s -> s.getSpimSource().getName().equals( name ) )
                .findFirst().get();
    }
}
