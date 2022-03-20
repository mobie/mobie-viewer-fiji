package org.embl.mobie.viewer.transform;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform2D;
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

public class TransformHelper
{
	public static RealInterval unionRealInterval( List< ? extends Source< ? > > sources )
	{
		RealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final FinalRealInterval bounds = MoBIEHelper.estimateBounds( source );

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
		final FinalRealInterval bounds = MoBIEHelper.estimateBounds( sourceAndConverter.getSpimSource() );
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

	public static double[] computeSourceUnionRealDimensions( List< SourceAndConverter< ? > > sources, double relativeMargin )
	{
		RealInterval bounds = unionRealInterval( sources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ));
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
			final double[] realDimensions = computeSourceUnionRealDimensions( sourceAndConverters, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN );
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

	public static AffineTransform3D asAffineTransform3D( AffineTransform2D affineTransform2D )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		// matrix
		for ( int r = 0; r < 2; r++ )
		{
			for ( int c = 0; c < 2; c++ )
			{
				affineTransform3D.set( affineTransform2D.get( r, c ), r, c);
			}
		}

		// translation
		for ( int r = 0; r < 2; r++ )
		{
			affineTransform3D.set( affineTransform2D.get( r, 2 ), r, 3 );
		}

		return affineTransform3D;
	}
}
