package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import ij.IJ;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.display.SourceDisplay;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.util.Set;
import java.util.function.Function;

public class SourceNamesRenderer implements TransformListener< AffineTransform3D >
{
	private final BdvHandle bdvHandle;

	public SourceNamesRenderer( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
		bdvHandle.getViewerPanel().addTransformListener( this );
	}

	@Override
	public void transformChanged( AffineTransform3D transform3D )
	{
		final ViewerPanel viewerPanel = bdvHandle.getViewerPanel();
		final ViewerState viewerState = viewerPanel.state().snapshot();

		final FinalRealInterval globalInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );

		final Set< SourceAndConverter< ? > > sources = viewerState.getVisibleAndPresentSources();

		final int t = viewerState.getCurrentTimepoint();
		final double expand = 0; //viewerState.getInterpolation() == Interpolation.NEARESTNEIGHBOR ? 0.5 : 1.0;

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		final double[] sourceMin = new double[ 3 ];
		final double[] sourceMax = new double[ 3 ];

		for ( final SourceAndConverter< ? > source : sources )
		{
			final Source< ? > spimSource = source.getSpimSource();
			final int level = 0; // spimSource.getNumMipmapLevels() - 1;
			spimSource.getSourceTransform( t, level, sourceToGlobal );

			final Interval interval = spimSource.getSource( t, level );
			for ( int d = 0; d < 3; d++ )
			{
				sourceMin[ d ] = interval.realMin( d ) - expand;
				sourceMax[ d ] = interval.realMax( d ) + expand;
			}
			final FinalRealInterval bb = sourceToGlobal.estimateBounds( new FinalRealInterval( sourceMin, sourceMax ) );

			if ( bb.realMax( 0 ) >= globalInterval.realMin(0 )
					&& bb.realMin( 0 ) <= globalInterval.realMax(0 )
					&& bb.realMax( 1 ) >= globalInterval.realMin(1 )
					&& bb.realMin( 1 ) <= globalInterval.realMax(2 )
					&& bb.realMax( 2 ) >= 0
					&& bb.realMin( 2 ) <= 0 )
			{
				System.out.println( spimSource.getName() );
			}
		}
	}
}
