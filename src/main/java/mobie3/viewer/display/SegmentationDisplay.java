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
package mobie3.viewer.display;

import bdv.viewer.Source;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mobie3.viewer.MoBIEHelper;
import mobie3.viewer.MultiThreading;
import mobie3.viewer.table.ColumnNames;
import mobie3.viewer.bdv.view.AnnotationSliceView;
import mobie3.viewer.bdv.view.AnnotatedLabelMaskSliceView;
import mobie3.viewer.segment.LabelToSegmentMapper;
import mobie3.viewer.serialize.SegmentationSource;
import mobie3.viewer.source.LazySpimSource;
import mobie3.viewer.source.SourceHelper;
import mobie3.viewer.table.AnnotatedSegment;
import mobie3.viewer.table.TableHelper;
import mobie3.viewer.table.TableRowsTableModel;
import mobie3.viewer.volume.SegmentsVolumeViewer;
import org.embl.mobie.io.util.IOHelper;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class SegmentationDisplay< AS extends AnnotatedSegment > extends AnnotationDisplay< AS >
{
	// Serialization
	protected List< String > sources;
	protected List< String > selectedSegmentIds;
	protected boolean showSelectedSegmentsIn3d = false;
	protected Double[] resolution3dView;

	// Runtime
	// TODO: below is almost not needed
	public transient LabelToSegmentMapper< AS > segmentMapper;
	public transient SegmentsVolumeViewer< AS > segmentsVolumeViewer;
	public transient AnnotatedLabelMaskSliceView sliceView;

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
		set( segmentationDisplay );

		this.sources = new ArrayList<>();
		this.sources.addAll( segmentationDisplay.nameToSourceAndConverter.keySet() );

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
	public void initTableRows( )
	{
		if ( getTables().size() == 0 ) return;

		tableModel = new TableRowsTableModel<>();

		// TODO: Don't load primary table but get it from the sac!
		// SourceHelper.unwrapSource( ... , SegmentsSource.class )

		// primary table (must contain columns to define image segments)
		final String primaryTable = tables.get( 0 );
		ArrayList< Future< ? > > futures = MultiThreading.getFutures();
		for ( String sourceName : sources )
		{
			Set< Source< ? > > rootSources = ConcurrentHashMap.newKeySet();
			SourceHelper.fetchRootSources( moBIE.sourceNameToSourceAndConverter().get( sourceName ).getSpimSource(), rootSources );

			// Merged grid source consists of multiple sources
			for ( Source< ? > spimSource : rootSources )
			{
				futures.add( MultiThreading.ioExecutorService.submit( () ->
				{
					if ( spimSource instanceof LazySpimSource )
					{
						( ( LazySpimSource ) spimSource ).getLazySourceAndConverterAndTables().setTableRootDirectory( moBIE.getTableRootDirectory( spimSource.getName() ) );
						( ( LazySpimSource ) spimSource ).getLazySourceAndConverterAndTables().setTableRows( tableModel );
						( ( LazySpimSource ) spimSource ).getLazySourceAndConverterAndTables().setPrimaryTable( primaryTable );
					}
					else
					{
						// TODO: get rid of this?! always use LazySpimSource?
						final List< TableRowImageSegment > tableRowImageSegments = moBIE.loadImageSegmentsTable( spimSource.getName(), primaryTable, "" );
						tableModel.addAll( tableRowImageSegments );
					}
				} ) );
			}
		}
		MultiThreading.waitUntilFinished( futures );

		// secondary table(s)
		for ( int i = 1; i < tables.size(); i++ )
		{
			final String tableName = tables.get( i );

			futures = MultiThreading.getFutures();
			for ( String sourceName : sources )
			{
				// code duplication (primary tables)
				Set< Source< ? > > rootSources = ConcurrentHashMap.newKeySet();
				SourceHelper.fetchRootSources( moBIE.sourceNameToSourceAndConverter().get( sourceName ).getSpimSource(), rootSources );

				for ( Source< ? > source : rootSources )
				{
					futures.add( MultiThreading.ioExecutorService.submit( () ->
					{
						if ( source instanceof LazySpimSource )
						{
							( ( LazySpimSource ) source ).getLazySourceAndConverterAndTables().addTable( tableName );
						}
						else
						{
							// TODO: get rid of this?! always use LazySpimSource?
							Map< String, List< String > > columns = openTable( tableName, sourceName );
							tableModel.mergeColumns( columns );
						}
					} ) );
				}
			}
		}

		if ( tableModel != null )
			segmentMapper = new LabelToSegmentMapper( tableModel.getTableRows() );
		else
			segmentMapper = new LabelToSegmentMapper();
	}

	@Override
	public void mergeColumns( String tableFileName )
	{
		// TODO: (maybe)
		//   - maybe rather first load all and then merge
		//   - multi-threading
		for ( String source : sources )
		{
			Map< String, List< String > > columns = openTable( tableFileName, source );
			tableModel.mergeColumns( columns );
		}
	}

	@Override
	public void mergeColumns( Map< String, List< String > > columns )
	{
		tableModel.mergeColumns( columns );
	}

	private Map< String, List< String > > openTable( String tableFileName, String source )
	{
		final String tablePath = moBIE.getTablePath( ( SegmentationSource ) moBIE.getDataSource( source ), tableFileName );
		Logger.log( "Opening table: " + tablePath );
		final Table table = Table.read().csv( IOHelper.getReader( tablePath ) );
		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );
		if ( ! columns.containsKey( ColumnNames.LABEL_IMAGE_ID ) )
			TableHelper.addColumn( columns, ColumnNames.LABEL_IMAGE_ID, source );

		// deal with the fact that the region ids are sometimes
		// stored as 1 and sometimes as 1.0
		// after below operation they all will be 1.0, 2.0, ...
		MoBIEHelper.toDoubleStrings( columns.get( ColumnNames.LABEL_ID ) );

		return  columns;
	}
}
