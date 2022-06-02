package org.embl.mobie.viewer.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerState;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SourceNamesRenderer extends BdvOverlay implements TransformListener< AffineTransform3D >
{
	private final BdvHandle bdvHandle;
	private Map< String, FinalRealInterval > sourceNameToBounds = new ConcurrentHashMap< String, FinalRealInterval >();
	private FinalRealInterval viewerInterval;

	public SourceNamesRenderer( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
		bdvHandle.getViewerPanel().addTransformListener( this );
		BdvFunctions.showOverlay(
				this,
				"sourceNameRenderer",
				BdvOptions.options().addTo( bdvHandle ) );
	}

	@Override
	public synchronized void transformChanged( AffineTransform3D transform3D )
	{
		sourceNameToBounds.clear();
		final ViewerState viewerState = bdvHandle.getViewerPanel().state().snapshot();

		final AffineTransform3D viewerTransform = viewerState.getViewerTransform();

		viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );

		final Set< SourceAndConverter< ? > > sources = viewerState.getVisibleAndPresentSources();

		final int t = viewerState.getCurrentTimepoint();

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
				sourceMin[ d ] = interval.realMin( d );
				sourceMax[ d ] = interval.realMax( d );
			}
			final FinalRealInterval sourceInterval = sourceToGlobal.estimateBounds( new FinalRealInterval( sourceMin, sourceMax ) );

			final FinalRealInterval intersect = Intervals.intersect( sourceInterval, viewerInterval );
			if ( ! Intervals.isEmpty( intersect ) )
			{
				final FinalRealInterval bounds = viewerTransform.estimateBounds( intersect );
				// If we want the name to be always visible
				// we could use intersect instead of bounds
				sourceNameToBounds.put( spimSource.getName(), bounds );
			}
		}
	}

	@Override
	protected void draw( Graphics2D g )
	{
		final double fieldOfViewWidth = viewerInterval.realMax( 0 ) - viewerInterval.realMin( 0 );
		for ( Map.Entry< String, FinalRealInterval > entry : sourceNameToBounds.entrySet() )
		{
			final FinalRealInterval sourceBounds = entry.getValue();
			final double sourceWidth = sourceBounds.realMax( 0 ) - sourceBounds.realMin( 0 );
			final double relativeWidth = sourceWidth / fieldOfViewWidth;
			final int fontSize = Math.min( 20,  (int) ( 20 * 4 * relativeWidth ));
			g.setFont( new Font( "TimesRoman", Font.PLAIN, fontSize ) );
			g.drawString( entry.getKey(), (int) sourceBounds.realMin( 0 ), (int) sourceBounds.realMax( 1 ) + fontSize );
		}
	}
}
