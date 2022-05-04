package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerState;
import bdv.viewer.render.ScreenScales;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.List;
import java.util.Set;

public class BdvSourcesInFieldOfViewListener
{
	public BdvSourcesInFieldOfViewListener( BdvHandle bdvHandle )
	{
		// TODO: Can I simplify the ScreenScales thing? What does it really do?
	}

	// copied from bdv.viewer.render.VisibilityUtils
	// because it has private access there

	private static void computeVisibleSourcesOnScreen(
			final ViewerState viewerState,
			final ScreenScales.ScreenScale screenScale,
			final List< SourceAndConverter< ? > > result )
	{
		result.clear();

		final int screenMinX = 0;
		final int screenMinY = 0;
		final int screenMaxX = screenScale.width() - 1;
		final int screenMaxY = screenScale.height() - 1;

		final AffineTransform3D screenTransform = viewerState.getViewerTransform();
		screenTransform.preConcatenate( screenScale.scaleTransform() );

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		final double[] sourceMin = new double[ 3 ];
		final double[] sourceMax = new double[ 3 ];

		final Set< SourceAndConverter< ? > > sources = viewerState.getVisibleAndPresentSources();
		final int t = viewerState.getCurrentTimepoint();
		final double expand = viewerState.getInterpolation() == Interpolation.NEARESTNEIGHBOR ? 0.5 : 1.0;

		for ( final SourceAndConverter< ? > source : sources )
		{
			if( !source.getSpimSource().doBoundingBoxCulling() )
			{
				result.add( source );
				continue;
			}

			final Source< ? > spimSource = source.getSpimSource();
			final int level = MipmapTransforms.getBestMipMapLevel( screenTransform, spimSource, t );
			spimSource.getSourceTransform( t, level, sourceToScreen );
			sourceToScreen.preConcatenate( screenTransform );

			final Interval interval = spimSource.getSource( t, level );
			for ( int d = 0; d < 3; d++ )
			{
				sourceMin[ d ] = interval.realMin( d ) - expand;
				sourceMax[ d ] = interval.realMax( d ) + expand;
			}
			final FinalRealInterval bb = sourceToScreen.estimateBounds( new FinalRealInterval( sourceMin, sourceMax ) );

			if ( bb.realMax( 0 ) >= screenMinX
					&& bb.realMin( 0 ) <= screenMaxX
					&& bb.realMax( 1 ) >= screenMinY
					&& bb.realMin( 1 ) <= screenMaxY
					&& bb.realMax( 2 ) >= 0
					&& bb.realMin( 2 ) <= 0 )
			{
				result.add( source );
			}
		}

		result.sort( viewerState.sourceOrder() );
	}
}
