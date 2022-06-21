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

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.color.SelectionColoringModel;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.plot.ScatterPlotViewer;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.AnnotationSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.table.TableRowsTableModel;
import org.embl.mobie.viewer.table.TableViewer;
import de.embl.cba.tables.color.ColoringLuts;
import org.embl.mobie.viewer.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AnnotationDisplay< T extends TableRow > extends AbstractSourceDisplay< AnnotationType< T > >
{
	// Serialization
	protected String lut = ColoringLuts.GLASBEY;
	protected String colorByColumn;
	protected Double[] valueLimits;
	protected boolean showScatterPlot = false;
	protected String[] scatterPlotAxes = new String[]{ TableColumnNames.ANCHOR_X, TableColumnNames.ANCHOR_Y };
	protected List< String > tables; // tables to display
	protected boolean showTable = true;
	protected boolean showAsBoundaries = false;
	protected float boundaryThickness = 1.0F;
	protected int randomColorSeed = 42;

	// Fixed
	protected transient final BlendingMode blendingMode = BlendingMode.SumOccluding;

	// Runtime
	public transient MoBIE moBIE;
	public transient SelectionModel< T > selectionModel;
	public transient SelectionColoringModel< T > selectionColoringModel;
	public transient TableViewer< T > tableViewer;
	public transient ScatterPlotViewer< T > scatterPlotViewer;
	public transient TableRowsTableModel< T > tableRows;

	// Should be overwritten by child classes
	public AnnotationSliceView< ? > getSliceView()
	{
		return null;
	}

	public abstract void initTableRows( );

	public abstract void mergeColumns( String tableColumns );

	public String getLut()
	{
		return lut;
	}

	public String getColorByColumn()
	{
		return colorByColumn;
	}

	public Double[] getValueLimits()
	{
		return valueLimits;
	}

	public boolean showScatterPlot()
	{
		return showScatterPlot;
	}

	public String[] getScatterPlotAxes()
	{
		return scatterPlotAxes;
	}

	public List< String > getTables()
	{
		return tables;
	}

	public boolean showTable()
	{
		return showTable;
	}

	public boolean isShowAsBoundaries()
	{
		return showAsBoundaries;
	}

	public float getBoundaryThickness()
	{
		return boundaryThickness;
	}

	public BlendingMode getBlendingMode()
	{
		return blendingMode;
	}

	public int getRandomColorSeed()
	{
		return randomColorSeed;
	}

	protected void set( AnnotationDisplay<T> annotationDisplay )
	{
		this.name = annotationDisplay.name;

		// Region displays only have one sourceAndConverter
		final SourceAndConverter< ? > sourceAndConverter = annotationDisplay.nameToSourceAndConverter.values().iterator().next();

		if( sourceAndConverter.getConverter() instanceof OpacityAdjuster )
		{
			final OpacityAdjuster opacityAdjuster = ( OpacityAdjuster ) sourceAndConverter.getConverter();
			this.opacity = opacityAdjuster.getOpacity();
		}

		this.lut = annotationDisplay.selectionColoringModel.getARGBLutName();

		final ColoringModel<T> wrappedColoringModel = annotationDisplay.selectionColoringModel.getWrappedColoringModel();

		if ( wrappedColoringModel instanceof ColumnColoringModel)
		{
			this.colorByColumn = (( ColumnColoringModel ) wrappedColoringModel).getColumnName();
		}

		if ( wrappedColoringModel instanceof NumericColoringModel)
		{
			this.valueLimits = new Double[2];
			NumericColoringModel numericColoringModel = ( NumericColoringModel ) ( wrappedColoringModel );
			this.valueLimits[0] = numericColoringModel.getMin();
			this.valueLimits[1] = numericColoringModel.getMax();
		}

		if ( wrappedColoringModel instanceof CategoryColoringModel )
		{
			this.randomColorSeed = ( ( CategoryColoringModel<?> ) wrappedColoringModel ).getRandomSeed();
		}

		this.showScatterPlot = annotationDisplay.scatterPlotViewer.isVisible();
		this.scatterPlotAxes = annotationDisplay.scatterPlotViewer.getSelectedColumns();
		this.tables = annotationDisplay.tables;
		List<String> additionalTables = annotationDisplay.tableViewer.getAdditionalTables();
		if ( additionalTables.size() > 0 ){
			if ( this.tables == null ) {
				this.tables = new ArrayList<>();
			}
			this.tables.addAll( additionalTables );
		}

		this.showTable = annotationDisplay.tableViewer.getWindow().isVisible();

		final AnnotationSource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), AnnotationSource.class );
		this.showAsBoundaries = boundarySource.isShowAsBoundaries();
		this.boundaryThickness = boundarySource.getBoundaryWidth();
	}

	public abstract void mergeColumns( Map< String, List< String > > columns );
}
