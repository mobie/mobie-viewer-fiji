/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.playground;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.util.ArrayList;

import static de.embl.cba.tables.Utils.getVoxelSpacings;


/**
 * These helper functions either exist already in bdv-playground,
 * but in a too recent version, or should be moved to bdv-playground.
 */
public class BdvPlaygroundHelper
{
	public static AffineTransform3D getViewerTransformWithNewCenter( BdvHandle bdvHandle, double[] position )
	{
		if ( position.length == 2 )
		{
			position = new double[]{
					position[ 0 ],
					position[ 1 ],
					0
			};
		}

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( currentViewerTransform );

		AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

		// ViewerTransform notes:
		// - applyInverse: coordinates in viewer => coordinates in image
		// - apply: coordinates in image => coordinates in viewer

		final double[] targetPositionInViewerInPixels = new double[ 3 ];
		currentViewerTransform.apply( position, targetPositionInViewerInPixels );

		for ( int d = 0; d < 3; d++ )
		{
			targetPositionInViewerInPixels[ d ] *= -1;
		}

		adaptedViewerTransform.translate( targetPositionInViewerInPixels );

		final double[] windowCentreInViewerInPixels = getWindowCentreInPixelUnits( bdvHandle.getViewerPanel() );

		adaptedViewerTransform.translate( windowCentreInViewerInPixels );

		return adaptedViewerTransform;
	}

	public static double[] getWindowCentreInPixelUnits( ViewerPanel viewerPanel )
	{
		final double[] windowCentreInPixelUnits = new double[ 3 ];
		windowCentreInPixelUnits[ 0 ] = viewerPanel.getDisplay().getWidth() / 2.0;
		windowCentreInPixelUnits[ 1 ] = viewerPanel.getDisplay().getHeight() / 2.0;
		return windowCentreInPixelUnits;
	}

	public static double[] getWindowCentreInCalibratedUnits( BdvHandle bdvHandle )
	{
		final double[] centreInPixelUnits = getWindowCentreInPixelUnits( bdvHandle.getViewerPanel() );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( affineTransform3D );
		final double[] centreInCalibratedUnits = new double[ 3 ];
		affineTransform3D.inverse().apply( centreInPixelUnits, centreInCalibratedUnits );
		return centreInCalibratedUnits;
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
