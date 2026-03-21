package org.embl.mobie.lib.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.util.Collections;
import java.util.Random;

public class ContrastComputer
{
    private final BdvHandle bdvHandle;
    private final SourceAndConverter< ? > sourceAndConverter;
    private ImagePlus imagePlus;

    public ContrastComputer( BdvHandle bdvHandle, SourceAndConverter< ? > sourceAndConverter )
    {
        this.bdvHandle = bdvHandle;
        this.sourceAndConverter = sourceAndConverter;
    }

    public double[] computeContrastLimitsWithinCurrentView( final int downSampling )
    {
        double viewerVoxelSpacing = BdvHandleHelper.getViewerVoxelSpacing( bdvHandle );
        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle, "" );
        screenShotMaker.run(
                Collections.singletonList( sourceAndConverter ),
                downSampling * viewerVoxelSpacing
        );
        imagePlus = screenShotMaker.getCompositeImagePlus();
        Roi[] rois = screenShotMaker.getMasks();
        if ( rois != null && rois.length > 0 )
            imagePlus.setRoi( rois[ 0 ] );
        IJ.run( imagePlus, "Enhance Contrast", "saturated=0.03" );
        double[] minMax = { imagePlus.getDisplayRangeMin(), imagePlus.getDisplayRangeMax() };
        return minMax;
    }

    public ImagePlus getImagePlus()
    {
        return imagePlus;
    }

    // useful if one needs to compute the contrast limits
    // before the image is visible in BDV
    public static double[] estimateMinMax( RandomAccessibleInterval<? extends RealType<?> > rai)
    {
        Cursor<? extends RealType<?>> cursor = rai.cursor();
        if (!cursor.hasNext()) return new double[]{0, 255};
        long stepSize = Intervals.numElements(rai) / 10000 + 1;
        int randomLimit = (int) Math.min(Integer.MAX_VALUE, stepSize);
        Random random = new Random(42);
        double min = cursor.next().getRealDouble();
        double max = min;
        while (cursor.hasNext()) {
            double value = cursor.get().getRealDouble();
            long steps = stepSize + random.nextInt( randomLimit );
            cursor.jumpFwd( steps );
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new double[]{min, max};
    }
}
