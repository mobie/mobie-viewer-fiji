/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import net.imglib2.type.numeric.integer.IntType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.annotate.RegionTableRow;
import org.embl.mobie.viewer.annotate.RegionImage;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.segment.SliceViewRegionSelector;
import org.embl.mobie.viewer.transform.SliceViewLocationChanger;
import org.embl.mobie.viewer.transform.PositionViewerTransform;

public class RegionSliceView extends AnnotationSliceView< RegionTableRow >
{
	public RegionSliceView( MoBIE moBIE, RegionDisplay display )
	{
		super( moBIE, display );
		SourceAndConverter< IntType > regionSourceAndConverter = createRegionSourceAndConverter();
		show( regionSourceAndConverter );
	}

	private SourceAndConverter< IntType > createRegionSourceAndConverter()
	{
		final RegionImage< RegionTableRow > intervalImage = new RegionImage( display.tableRows.getTableRows(), display.selectionColoringModel, display.getName() );

		return intervalImage.getSourceAndConverter();
	}

	@Override
	public void focusEvent( RegionTableRow selection, Object initiator )
	{
		if ( initiator instanceof SliceViewRegionSelector )
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		state.setCurrentTimepoint( selection.timePoint() );

		final double[] center = getPosition( selection );

		SliceViewLocationChanger.changeLocation( bdvHandle, new PositionViewerTransform( center, state.getCurrentTimepoint() ) );
	}

	private double[] getPosition( RegionTableRow selection )
	{
		final double[] max = selection.mask().maxAsDoubleArray();
		final double[] min = selection.mask().minAsDoubleArray();
		final double[] center = new double[ min.length ];
		for ( int d = 0; d < 3; d++ )
		{
			center[ d ] = ( max[ d ] + min[ d ] ) / 2;
		}
		return center;
	}
}
