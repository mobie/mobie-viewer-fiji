package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getLevel;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerVoxelSpacing;

public class SourceViewRasterizer
{
	private final BdvHandle bdvHandle;
	private final Source< ? > source;

	private RandomAccessibleInterval< FloatType > rasterizedSourceView;

	public SourceViewRasterizer( BdvHandle bdvHandle, Source< ? > source )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
	}

	public RandomAccessibleInterval< FloatType > getRasterizedSourceView()
	{
		if ( rasterizedSourceView == null )
			rasterizedSourceView = raster( bdvHandle, source );

		return rasterizedSourceView;
	}

	private RandomAccessibleInterval< FloatType > raster( BdvHandle bdvHandle, Source< ? > source )
	{
		final int t = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
		final double samplingPhysical = BdvUtils.getViewerVoxelSpacing( bdvHandle );
		long[] sizePixels = ScreenShotMaker.getCaptureImageSizeInPixels( bdvHandle, samplingPhysical );

		final RandomAccessibleInterval< FloatType > rai = ArrayImgs.floats( sizePixels[ 0 ], sizePixels[ 1 ] );

		final int level = getLevel( source, samplingPhysical );

		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( t, level, sourceTransform );
		final AffineTransform3D viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform();

		AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
		viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
		viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

		final double canvasStepSize = samplingPhysical / getViewerVoxelSpacing( bdvHandle );

		Grids.collectAllContainedIntervals(
				Intervals.dimensionsAsLongArray( rai ),
				new int[]{200, 200}).parallelStream().forEach( interval ->
		{
			RealRandomAccess< ? extends RealType< ? > > realTypeAccess =
					(RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NLINEAR).realRandomAccess();

			final IntervalView< FloatType > crop = Views.interval( rai, interval );
			final Cursor< FloatType > cursor = Views.iterable( crop ).localizingCursor();
			final RandomAccess< FloatType > access = crop.randomAccess();

			final double[] canvasPosition = new double[ 3 ];
			final double[] sourcePixelPosition = new double[ 3 ];

			// iterate through the target image in pixel units
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.localize( canvasPosition );
				access.setPosition( cursor );

				// canvasPosition is the position on the canvas, in calibrated units
				// dxy is the step size that is needed to get
				// the desired resolution in the output image
				canvasPosition[ 0 ] *= canvasStepSize;
				canvasPosition[ 1 ] *= canvasStepSize;

				viewerToSourceTransform.apply( canvasPosition, sourcePixelPosition );

				access.get().setReal( realTypeAccess.setPositionAndGet( sourcePixelPosition ).getRealFloat() );

			}
		});
		return rai;
	}
}
