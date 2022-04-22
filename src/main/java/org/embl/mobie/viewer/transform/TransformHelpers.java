package org.embl.mobie.viewer.transform;

import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.Scale3D;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.playground.BdvPlaygroundUtils;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransformHelpers
{
	public static RealInterval estimateBounds( List< ? extends Source< ? > > sources, int t )
	{
		RealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final RealInterval bounds = MoBIEHelper.estimateBounds( source, t );

			if ( union == null )
				union = bounds;
			else
				union = Intervals.union( bounds, union );
		}

		return union;
	}


	public static SourceAndConverter< ? > centerAtOrigin( SourceAndConverter< ? > sourceAndConverter )
	{
		final AffineTransform3D translate = new AffineTransform3D();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );
		final double[] center = getCenter( sourceAndConverter );
		translate.translate( center );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( translate.inverse() );
		sourceAndConverter = transformer.apply( sourceAndConverter );
		return sourceAndConverter;
	}

	public static double[] getCenter( SourceAndConverter< ? > sourceAndConverter )
	{
		final RealInterval bounds = MoBIEHelper.estimateBounds( sourceAndConverter.getSpimSource(), 0 );
		final double[] center = bounds.minAsDoubleArray();
		final double[] max = bounds.maxAsDoubleArray();
		for ( int d = 0; d < max.length; d++ )
		{
			center[ d ] = ( center[ d ] + max[ d ] ) / 2;
		}
		return center;
	}

	public static AffineTransform3D createTranslationTransform3D( double translationX, double translationY, SourceAndConverter< ? > sourceAndConverter, boolean centerAtOrigin )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = getCenter( sourceAndConverter );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		translationTransform.translate( translationX, translationY, 0 );
		return translationTransform;
	}

	public static double[] computeSourceUnionRealDimensions( List< SourceAndConverter< ? > > sources, double relativeMargin, int t )
	{
		RealInterval bounds = estimateBounds( sources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), t );
		final double[] realDimensions = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			realDimensions[ d ] = ( 1.0 + 2.0 * relativeMargin ) * ( bounds.realMax( d ) - bounds.realMin( d ) );
		return realDimensions;
	}

	public static double[] getMaximalSourceUnionRealDimensions( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, Collection< List< String > > sourceNamesList )
	{
		double[] maximalDimensions = new double[ 2 ];
		for ( List< String > sourceNames : sourceNamesList )
		{
			final List< SourceAndConverter< ? > > sourceAndConverters = sourceNames.stream().map( name -> sourceNameToSourceAndConverter.get( name ) ).collect( Collectors.toList() );
			final double[] realDimensions = computeSourceUnionRealDimensions( sourceAndConverters, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN, 0 );
			for ( int d = 0; d < 2; d++ )
				maximalDimensions[ d ] = realDimensions[ d ] > maximalDimensions[ d ] ? realDimensions[ d ] : maximalDimensions[ d ];
		}

		return maximalDimensions;
	}

	public static AffineTransform3D createNormalisedViewerTransform( ViewerPanel viewerPanel )
	{
		return createNormalisedViewerTransform( viewerPanel, BdvPlaygroundUtils.getWindowCentreInPixelUnits( viewerPanel ) );
	}

	public static AffineTransform3D createNormalisedViewerTransform( ViewerPanel viewerPanel, double[] position )
	{
		final AffineTransform3D view = new AffineTransform3D();
		viewerPanel.state().getViewerTransform( view );

		// translate position to upper left corner of the Window (0,0)
		final AffineTransform3D translate = new AffineTransform3D();
		translate.translate( position );
		view.preConcatenate( translate.inverse() );

		// divide by window width
		final int bdvWindowWidth = viewerPanel.getDisplay().getWidth();;
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		view.preConcatenate( scale );

		return view;
	}

	public static AffineTransform3D createUnnormalizedViewerTransform( AffineTransform3D normalisedTransform, ViewerPanel viewerPanel )
	{
		final AffineTransform3D transform = normalisedTransform.copy();

		final int bdvWindowWidth = viewerPanel.getDisplay().getWidth();
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		transform.preConcatenate( scale.inverse() );

		AffineTransform3D translate = new AffineTransform3D();
		translate.translate( BdvPlaygroundUtils.getWindowCentreInPixelUnits( viewerPanel ) );

		transform.preConcatenate( translate );

		return transform;
	}

	public static AffineTransform3D getIntervalViewerTransform( BdvHandle bdv, RealInterval interval  )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];

		for( int d = 0; d < 3; ++d )
		{
			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			centerPosition[ d ] = - center;
		}

		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		// TODO: check both dimensions and take the smaller scale
		double scale = Double.MAX_VALUE;
		for ( int d = 0; d < 2; d++ )
		{
			final double size = interval.realMax( d ) - interval.realMin( d );
			scale = Math.min( scale, 1.0 * bdvWindowDimensions[ d ] / size );
		}

		affineTransform3D.scale( scale );

		double[] shiftToBdvWindowCenter = new double[ 3 ];

		for( int d = 0; d < 2; ++d )
		{
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		}

		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}
}
