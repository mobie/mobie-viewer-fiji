package org.embl.mobie.viewer.playground;

import bdv.SpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.transform.MergedGridSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.embl.cba.tables.Utils.getVoxelSpacings;


/**
 * These helper functions either exist already in bdv-playground,
 * but in a too recent version, or should be moved to bdv-playground.
 */
public class BdvPlaygroundUtils
{
	public static double[] getWindowCentreInPixelUnits( BdvHandle bdvHandle )
	{
		final double[] windowCentreInPixelUnits = new double[ 3 ];
		windowCentreInPixelUnits[ 0 ] = bdvHandle.getViewerPanel().getDisplay().getWidth() / 2.0;
		windowCentreInPixelUnits[ 1 ] = bdvHandle.getViewerPanel().getDisplay().getHeight() / 2.0;
		return windowCentreInPixelUnits;
	}

	public static double[] getWindowCentreInCalibratedUnits( BdvHandle bdvHandle )
	{
		final double[] centreInPixelUnits = getWindowCentreInPixelUnits( bdvHandle );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( affineTransform3D );
		final double[] centreInCalibratedUnits = new double[ 3 ];
		affineTransform3D.inverse().apply( centreInPixelUnits, centreInCalibratedUnits );
		return centreInCalibratedUnits;
	}

	/**
	 * Recursively fetch the names of all root sources
	 * @param source
	 * @param rootSources
	 */
	public static void fetchRootSources( Source< ? > source, Set< Source > rootSources )
	{
		if ( source instanceof SpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof MergedGridSource )
		{
			final MergedGridSource< ? > mergedGridSource = ( MergedGridSource ) source;
			final List< ? extends Source< ? > > gridSources = mergedGridSource.getGridSources();
			for ( Source< ? > gridSource : gridSources )
			{
				fetchRootSources( gridSource, rootSources );
			}
		}
		else if (  source instanceof ResampledSource )
		{
			final ResampledSource resampledSource = ( ResampledSource ) source;
			final Source< ? > wrappedSource = resampledSource.getOriginalSource();
			fetchRootSources( wrappedSource, rootSources );
		}
		else
		{
			throw new IllegalArgumentException("For sources of type " + source.getClass().getName() + " the root source currently cannot be determined.");
		}
	}

	public static int getLevel( Source< ? > source, long maxNumVoxels )
	{
		final ArrayList< double[] > voxelSpacings = getVoxelSpacings( source );

		final int numLevels = voxelSpacings.size();

		int level;

		for ( level = 0; level < numLevels; level++ )
		{
			final long numElements = Intervals.numElements( source.getSource( 0, level ) );

			if ( numElements <= maxNumVoxels )
				break;
		}

		if ( level == numLevels ) level = numLevels - 1;

		return level;
	}

	public static int getLevel( Source< ? > source, double[] requestedVoxelSpacing )
	{
		ArrayList< double[] > voxelSpacings = getVoxelSpacings( source );
		return getLevel( voxelSpacings, requestedVoxelSpacing );
	}

	public static int getLevel( ArrayList< double[] > sourceVoxelSpacings, double[] requestedVoxelSpacing )
	{
		int level;
		int numLevels = sourceVoxelSpacings.size();
		final int numDimensions = sourceVoxelSpacings.get( 0 ).length;

		for ( level = 0; level < numLevels; level++ )
		{
			boolean allLargerOrEqual = true;
			for ( int d = 0; d < numDimensions; d++ )
			{
				if ( sourceVoxelSpacings.get( level )[ d ] < requestedVoxelSpacing[ d ] )
				{
					allLargerOrEqual = false;
					continue;
				}
			}

			if ( allLargerOrEqual ) break;
		}

		if ( level == numLevels ) level = numLevels - 1;

		return level;
	}
}
