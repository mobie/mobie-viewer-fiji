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
package org.embl.mobie.viewer.display;

import bdv.viewer.Source;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.MultiThreading;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.segment.SegmentAdapter;
import org.embl.mobie.viewer.bdv.view.SegmentationSliceView;
import org.embl.mobie.viewer.source.LazySpimSource;
import org.embl.mobie.viewer.source.SegmentationSource;
import org.embl.mobie.viewer.table.TableHelper;
import org.embl.mobie.viewer.table.TableRowsTableModel;
import org.embl.mobie.viewer.volume.SegmentsVolumeViewer;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class SegmentationDisplay extends AnnotationDisplay< TableRowImageSegment >
{
	// Serialization
	protected List< String > sources;
	protected List< String > selectedSegmentIds;
	protected boolean showSelectedSegmentsIn3d = false;
	protected Double[] resolution3dView;

	// Runtime
	public transient SegmentAdapter< TableRowImageSegment > segmentAdapter;
	public transient SegmentsVolumeViewer< TableRowImageSegment > segmentsVolumeViewer;
	public transient SegmentationSliceView sliceView;

	@Override
	public AnnotationSliceView< ? > getSliceView()
	{
		return sliceView;
	}

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}

	public Double[] getResolution3dView(){ return resolution3dView; }

	public List< String > getSelectedSegmentIds()
	{
		return selectedSegmentIds;
	}

	public boolean showSelectedSegmentsIn3d()
	{
		return showSelectedSegmentsIn3d;
	}

	// Needed for Gson
	public SegmentationDisplay(){}

	public SegmentationDisplay( String name, double opacity, List< String > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showSelectedSegmentsIn3d, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, Double[] resolution3dView )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedSegmentIds = selectedSegmentIds;
		this.showSelectedSegmentsIn3d = showSelectedSegmentsIn3d;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
		this.resolution3dView = resolution3dView;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param segmentationDisplay
	 */
	public SegmentationDisplay( SegmentationDisplay segmentationDisplay )
	{
		setAnnotationSettings( segmentationDisplay );

		this.sources = new ArrayList<>();
		this.sources.addAll( segmentationDisplay.displayedSourceNameToSourceAndConverter.keySet() );

		if ( segmentationDisplay.segmentsVolumeViewer != null )
		{
			this.showSelectedSegmentsIn3d = segmentationDisplay.segmentsVolumeViewer.isShowSegments();

			double[] voxelSpacing = segmentationDisplay.segmentsVolumeViewer.getVoxelSpacing();
			if ( voxelSpacing != null ) {
				resolution3dView = new Double[voxelSpacing.length];
				for (int i = 0; i < voxelSpacing.length; i++) {
					resolution3dView[i] = voxelSpacing[i];
				}
			}
		}

		Set<TableRowImageSegment> currentSelectedSegments = segmentationDisplay.selectionModel.getSelected();
		if (currentSelectedSegments != null) {
			ArrayList<String> selectedSegmentIds = new ArrayList<>();
			for (TableRowImageSegment segment : currentSelectedSegments) {
				selectedSegmentIds.add( segment.imageId() + ";" + segment.timePoint() + ";" + (int) segment.labelId() );
			}
			this.selectedSegmentIds = selectedSegmentIds;
		}

		if ( segmentationDisplay.sliceView != null ) {
			visible = segmentationDisplay.sliceView.isVisible();
		}
	}

	// It is important that this is called after
	// all the sourceAndConverter are registered
	// in MoBIE
	public void initTableRows( MoBIE moBIE )
	{
		if ( getTables().size() == 0 ) return;

		tableRows = new TableRowsTableModel<>();

		// primary table (must contain columns to define image segments)
		final String primaryTable = tables.get( 0 );
		ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( String sourceName : sources )
		{
			Set< Source< ? > > rootSources = ConcurrentHashMap.newKeySet();
			MoBIEHelper.fetchRootSources( moBIE.sourceNameToSourceAndConverter().get( sourceName ).getSpimSource(), rootSources );

			for ( Source< ? > source : rootSources )
			{
				futures.add( MultiThreading.ioExecutorService.submit( () ->
				{
					if ( source instanceof LazySpimSource )
					{
						( ( LazySpimSource ) source ).setTableRootDirectory( moBIE.getTableRootDirectory( source.getName() ) );
						( ( LazySpimSource ) source ).setTableRows( tableRows );
						( ( LazySpimSource ) source ).setPrimaryTable( primaryTable );
					}
					else
					{
						final List< TableRowImageSegment > tableRowImageSegments = moBIE.loadImageSegmentsTable( source.getName(), primaryTable, "" );
						tableRows.addAll( tableRowImageSegments );
					}
				} ) );
			}
		}
		MultiThreading.waitUntilFinished( futures );

		if ( tables.size() == 1 )
			return; // only primary table

		// secondary table(s)
		for ( int i = 1; i < tables.size(); i++ )
		{
			final String tableName = tables.get( i );

			futures = MultiThreading.getFutures();
			for ( String sourceName : sources )
			{
				// code duplication (primary tables)
				Set< Source< ? > > rootSources = ConcurrentHashMap.newKeySet();
				MoBIEHelper.fetchRootSources( moBIE.sourceNameToSourceAndConverter().get( sourceName ).getSpimSource(), rootSources );

				for ( Source< ? > source : rootSources )
				{
					futures.add( MultiThreading.ioExecutorService.submit( () ->
					{
						if ( source instanceof LazySpimSource )
						{
							( ( LazySpimSource ) source ).addTable( tableName );
						}
						else
						{
							Map< String, List< String > > columns = TableHelper.loadTableAndAddImageIdColumn( sourceName, moBIE.getTablePath( ( SegmentationSource ) moBIE.getImageSource( sourceName ), tableName ) );
							tableRows.mergeColumns( columns );
						}
					} ) );
				}
			}
		}

		// TODO move to primary table loading
//		for ( TableRowImageSegment segment : segmentationDisplay.tableRows )
//		{
//			if ( segment.labelId() == 0 )
//			{
//				throw new UnsupportedOperationException( "The table contains rows (image segments) with label index 0, which is not supported and will lead to errors. Please change the table accordingly." );
//			}
//		}
	}
}
