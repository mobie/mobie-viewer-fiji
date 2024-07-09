package org.embl.mobie.lib.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
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
        screenShotMaker.run( Collections.singletonList( sourceAndConverter ), 4 * viewerVoxelSpacing );
        ImagePlus imagePlus = screenShotMaker.getCompositeImagePlus();
        IJ.run(imagePlus, "Enhance Contrast", "saturated=0.35");
        return new double[]{ imagePlus.getDisplayRangeMin(), imagePlus.getDisplayRangeMax() };
    }
}
