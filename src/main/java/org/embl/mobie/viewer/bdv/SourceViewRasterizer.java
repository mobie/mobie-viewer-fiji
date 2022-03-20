package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.bdv.utils.BdvUtils.getLevel;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerVoxelSpacing;

public class SourceViewRasterizer
{
	private final BdvHandle bdvHandle;
	private final List< Source< ? > > sources;

	private int currentTimepoint;
	private double rasterVoxelSize;
	private ArrayList< RandomAccessibleInterval< FloatType > > rasterRais;
	private long[] rasterDimensions;
	private AffineTransform3D rasterTransform;

	public SourceViewRasterizer( BdvHandle bdvHandle, List< Source< ? > > sources )
	{
		this.bdvHandle = bdvHandle;
		this.sources = sources;
		currentTimepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
	}

	public List< RandomAccessibleInterval< FloatType > > getRasterRais()
	{
		if ( rasterRais == null )
			raster();

		return rasterRais;
	}
	
	public double getRasterVoxelSize()
	{
		return rasterVoxelSize;
	}

	public AffineTransform3D getRasterTransform()
	{
		return rasterTransform;
	}

	private void raster()
	{
		rasterTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
		rasterVoxelSize = determineRasterVoxelSize();
		rasterDimensions = ScreenShotMaker.captureImageSizeInPixels( bdvHandle, rasterVoxelSize );
		rasterRais = new ArrayList<>();

		for ( Source< ? > source : sources )
		{
			final RandomAccessibleInterval< FloatType > rai = raster( source );
			rasterRais.add( rai );
		}
	}

	private RandomAccessibleInterval< FloatType > raster( Source< ? > source )
	{
		final RandomAccessibleInterval< FloatType > rai = ArrayImgs.floats( rasterDimensions[ 0 ], rasterDimensions[ 1 ] );
		final int level = getLevel( source, rasterVoxelSize );
		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( currentTimepoint, level, sourceTransform );

		AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
		viewerToSourceTransform.preConcatenate( rasterTransform.inverse() );
		viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

		final double canvasStepSize = rasterVoxelSize / getViewerVoxelSpacing( bdvHandle );

		RealRandomAccess< ? extends RealType< ? > > realTypeAccess = (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource( currentTimepoint, level, Interpolation.NLINEAR).realRandomAccess();

		// TODO: Make below code work (i.e. render in ImageJ or BDVFunctions)
		final RandomAccessibleInterval< ? > interval = Views.interval(
				Views.raster(
						RealViews.affine( source.getInterpolatedSource( currentTimepoint, level, Interpolation.NLINEAR ), viewerToSourceTransform )
				),
				new FinalInterval( bdvHandle.getViewerPanel().getWidth(), bdvHandle.getViewerPanel().getHeight() ) );
		//BdvFunctions.show( interval, "" );
		// ImageJFunctions.show( (RandomAccessibleInterval) interval, "" );


		final Cursor< FloatType > cursor = Views.iterable( rai ).localizingCursor();
		final RandomAccess< FloatType > access = rai.randomAccess();

		final double[] canvasPosition = new double[ 3 ];
		final double[] sourcePixelPosition = new double[ 3 ];

		// iterate through the target image in pixel units
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( canvasPosition );
			access.setPosition( cursor );

			// canvasPosition is the position on the canvas
			// in physical units
			// dxy is the step size that is needed to get
			// the desired resolution in the output image
			canvasPosition[ 0 ] *= canvasStepSize;
			canvasPosition[ 1 ] *= canvasStepSize;

			viewerToSourceTransform.apply( canvasPosition, sourcePixelPosition );
			access.get().setReal( realTypeAccess.setPositionAndGet( sourcePixelPosition ).getRealFloat() );
		}

		return rai;
	}

	private double determineRasterVoxelSize()
	{
		double rasterVoxelSize = BdvUtils.getViewerVoxelSpacing( bdvHandle );
		for ( Source< ? > source : sources )
		{
			final int level = getLevel( source, rasterVoxelSize );
			final double sourceVoxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize( source, currentTimepoint, level );
			rasterVoxelSize = Math.max( rasterVoxelSize, sourceVoxelSize );
		}
		return BdvUtils.getViewerVoxelSpacing( bdvHandle );
	}
}
