package org.embl.mobie.lib.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.util.Collections;

public class AutoContrastAdjuster
{
    private final BdvHandle bdvHandle;
    private final SourceAndConverter< ? > sourceAndConverter;


    public AutoContrastAdjuster( BdvHandle bdvHandle, SourceAndConverter< ? > sourceAndConverter )
    {
        this.bdvHandle = bdvHandle;
        this.sourceAndConverter = sourceAndConverter;
    }

    public double[] computeMinMax()
    {
        double viewerVoxelSpacing = BdvHandleHelper.getViewerVoxelSpacing( bdvHandle );
        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, "" );
        screenShotMaker.run(
                Collections.singletonList( sourceAndConverter ),
                16 * viewerVoxelSpacing // times 16 to make it faster
        );
        ImagePlus imagePlus = screenShotMaker.getCompositeImagePlus();
        Roi[] rois = screenShotMaker.getMasks();
        if ( rois != null && rois.length > 0 )
            imagePlus.setRoi( rois[ 0 ] );
        IJ.run( imagePlus, "Enhance Contrast", "saturated=0.03" );
        // imagePlus.show();
        double[] minMax = { imagePlus.getDisplayRangeMin(), imagePlus.getDisplayRangeMax() };
        return minMax;
    }
}
