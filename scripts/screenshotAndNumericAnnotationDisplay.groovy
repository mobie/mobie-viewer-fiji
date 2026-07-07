import bdv.util.BdvHandle
import bdv.viewer.SourceAndConverter
import ij.IJ
import net.imglib2.type.numeric.ARGBType
import org.embl.mobie.MoBIE
import org.embl.mobie.command.context.ScreenShotStackMakerCommand
import org.embl.mobie.lib.transform.viewer.ViewerTransform
import org.embl.mobie.lib.transform.viewer.ViewerTransformChanger

// Launch MoBIE
def moBIE = new MoBIE("https://github.com/mobie/platybrowser-project")

// Obtain a handle on BigDataViewer
def bdvHandle = moBIE.getViewManager().getSliceViewer().getBdvHandle()

// Load and show an additional view
// moBIE.getViewManager().getAdditionalViewsLoader().loadViews( "my-views.json")
// moBIE.getViewManager().show( "my-view-name" );

// To obtain such viewer transforms:
// Right click in BDV and select "Log Current Location"
// In the ImageJ Log window it will print: {"normalizedAffine":[0.05152857716346159,0.0,0.0,-5.751562308992142,0.0,0.05152857716346159,0.0,-7.918098346939994,0.0,0.0,0.05152857716346159,-5.541408380633763],"timepoint":0}
def viewerTransform = ViewerTransform.toViewerTransform(
        '{"normalizedAffine":[0.05152857716346159,0.0,0.0,-5.751562308992142,0.0,0.05152857716346159,0.0,-7.918098346939994,0.0,0.0,0.05152857716346159,-5.541408380633763],"timepoint":0}'
)
ViewerTransformChanger.apply(bdvHandle, viewerTransform)

// Show ProSPr signal for a muscle gene
moBIE.getViewManager().show("mhcl4")

// Demonstrate how to create a screenshot and save it
def command = new ScreenShotStackMakerCommand()
command.bdvHandle = bdvHandle
command.numSlices = 5
command.targetSamplingInXY = 0.2 // micrometer
command.targetSamplingInZ = 0.5 // micrometer
command.showImages = false // don't show, as we will save the image to disk instead
command.run()

// Get the screenshot and save it
def rgbImg = command.getRgbImg()
IJ.save(rgbImg, "/Users/tischer/Downloads/platy-screenshot-stack.tif")

// Create a new numeric annotation image display

// Add cell segmentation
moBIE.getViewManager().show("cells")
// and hide it
def cellsSource = getSource( bdvHandle, "cells" );
bdvHandle.getViewerPanel().state().setSourceActive( cellsSource, false );

// Create a new numeric annotation image display...
def displays = moBIE.getViewManager().getCurrentSegmentationDisplays()
def imageName = displays.get("cells").getTableView().createNumericAnnotationDisplay("anchor_x")

// ...and change the display settings
def source = getSource(bdvHandle, imageName)

def red = new ARGBType(ARGBType.rgba(255, 0, 0, 255))
def converterSetup = bdvHandle.getConverterSetups().getConverterSetup(source)
converterSetup.setColor(red)
converterSetup.setDisplayRange(100, 200)
bdvHandle.getViewerPanel().requestRepaint()


private static SourceAndConverter< ? > getSource(BdvHandle bdvHandle, final String name)
{
    return bdvHandle.getViewerPanel().state().getSources().stream()
            .filter( s -> s.getSpimSource().getName().equals( name ) )
            .findFirst().get();
}
