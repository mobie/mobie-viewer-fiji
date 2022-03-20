package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;

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
	private boolean raster2DSources;
	private boolean showRasterImage;

	/**
	 * @param bdvHandle
	 * @param sources
	 * @param raster2DSources    If true, raster 2-D sources as if they were in the viewer xy plane. This is useful, e.g., to rasterize serial sections that are not visible in the same z-plane.
	 * @param showRasterImage
	 */
	public SourceViewRasterizer( BdvHandle bdvHandle, List< Source< ? > > sources, boolean raster2DSources, boolean showRasterImage )
	{
		this.bdvHandle = bdvHandle;
		this.sources = sources;
		this.raster2DSources = raster2DSources;
		this.showRasterImage = showRasterImage;
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
		currentTimepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
		rasterTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
		// TODO: the two variables below are not really used anymore
		//  they were only needed for the rasterViaCursor function, which
		//  currently is not used.
		rasterVoxelSize = determineRasterVoxelSize();
		rasterDimensions = ScreenShotMaker.captureImageSizeInPixels( bdvHandle, rasterVoxelSize );
		rasterRais = new ArrayList<>();

		for ( Source< ? > source : sources )
		{
			final RandomAccessibleInterval< FloatType > rai = raster( ( Source< ? extends RealType< ? > > ) source );
			rasterRais.add( rai );
		}
	}

	private RandomAccessibleInterval< FloatType > raster( Source< ? extends RealType< ? > > source )
	{
		final int level = getLevel( source, rasterVoxelSize );

		AffineTransform3D rasterToSourceTransform = getRasterToSourceTransform( source, level, raster2DSources );

		final RandomAccessibleInterval< FloatType > rai = raster( source, level, rasterToSourceTransform );

		//final RandomAccessibleInterval< FloatType > rai = rasterViaCursor( rasterToSourceTransform, source, level );

		return rai;
	}

	private RandomAccessibleInterval< FloatType > raster( Source< ? extends RealType< ? > > source, int level, AffineTransform3D rasterToSourceTransform )
	{
		final RandomAccessible< ? extends RealType< ? > > ra = Views.raster( RealViews.affine( ( source.getInterpolatedSource( currentTimepoint, level, Interpolation.NLINEAR ) ), rasterToSourceTransform.inverse() ) );

		final FinalInterval interval = new FinalInterval( bdvHandle.getViewerPanel().getWidth(), bdvHandle.getViewerPanel().getHeight(), 1 );

		RandomAccessibleInterval< ? extends RealType< ? > > rai = Views.interval( ra, interval );

		rai = Views.dropSingletonDimensions( rai );

		final RandomAccessibleInterval< FloatType > floatTypeRai = Converters.convert( rai, ( i, o ) -> o.setReal( i.getRealFloat() ), new FloatType() );

		if ( showRasterImage )
			ImageJFunctions.show( floatTypeRai, source.getName() );

		return Views.dropSingletonDimensions( floatTypeRai );
	}

	private AffineTransform3D getRasterToSourceTransform( Source< ? > source, int level, boolean ignoreZ )
	{
		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( currentTimepoint, level, sourceTransform );
		AffineTransform3D rasterToSourceTransform = new AffineTransform3D();
		rasterToSourceTransform.preConcatenate( rasterTransform.inverse() );
		rasterToSourceTransform.preConcatenate( sourceTransform.inverse() );
		if ( ignoreZ )
			rasterToSourceTransform.set( 0.0, 2, 3 );
		return rasterToSourceTransform;
	}

	private RandomAccessibleInterval< FloatType > rasterViaCursor( AffineTransform3D viewerToSourceTransform, Source< ? > source, int level )
	{
		final RandomAccessibleInterval< FloatType > rai = ArrayImgs.floats( rasterDimensions[ 0 ], rasterDimensions[ 1 ] );

		final double canvasStepSize = rasterVoxelSize / getViewerVoxelSpacing( bdvHandle );

		RealRandomAccess< ? extends RealType< ? > > realTypeAccess = (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource( currentTimepoint, level, Interpolation.NLINEAR).realRandomAccess();


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
		// TODO: the current code "rasterViaView" requires that the
		//  rasterVoxelSize is equal to the voxelSize in the BDV viewer!
		//  Otherwise the returned rasterTransform would be wrong.
		//  If other sampling is needed this should probably be done by
		//  changing the rasterTransform.
//		double rasterVoxelSize = BdvUtils.getViewerVoxelSpacing( bdvHandle );
//		for ( Source< ? > source : sources )
//		{
//			final int level = getLevel( source, rasterVoxelSize );
//			final double sourceVoxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize( source, currentTimepoint, level );
//			rasterVoxelSize = Math.max( rasterVoxelSize, sourceVoxelSize );
//		}
		return BdvUtils.getViewerVoxelSpacing( bdvHandle );
	}
}
