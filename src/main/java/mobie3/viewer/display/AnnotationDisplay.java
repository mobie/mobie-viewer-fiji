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

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import mobie3.viewer.MoBIE;
import mobie3.viewer.bdv.render.BlendingMode;
import mobie3.viewer.bdv.view.AnnotationSliceView;
import mobie3.viewer.color.OpacityAdjuster;
import mobie3.viewer.color.SelectionColoringModel;
import mobie3.viewer.plot.ScatterPlotView;
import mobie3.viewer.select.SelectionModel;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.SourceHelper;
import mobie3.viewer.table.Annotation;
import mobie3.viewer.table.AnnotationTableModel;
import mobie3.viewer.table.ColumnNames;
import mobie3.viewer.table.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AnnotationDisplay< A extends Annotation > extends AbstractDisplay< A >
{
	// Serialization
	protected String lut = ColoringLuts.GLASBEY;
	protected String colorByColumn;
	protected Double[] valueLimits;
	protected boolean showScatterPlot = false;
	protected String[] scatterPlotAxes = new String[]{ ColumnNames.ANCHOR_X, ColumnNames.ANCHOR_Y };
	protected List< String > tables; // tables to display
	protected boolean showTable = true;
	protected boolean showAsBoundaries = false;
	protected float boundaryThickness = 1.0F;
	protected int randomColorSeed = 42;

	// Fixed
	protected transient final BlendingMode blendingMode = BlendingMode.SumOccluding;

	// Runtime
	public transient MoBIE moBIE;
	public transient SelectionModel< A > selectionModel;
	public transient SelectionColoringModel< A > coloringModel;
	public transient TableView< A > tableView;
	public transient ScatterPlotView< A > scatterPlotView;
	public transient AnnotationTableModel< A > tableModel;

	public abstract AnnotationSliceView< ? > getSliceView();

	public abstract void initTableModel( );

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

	protected void set( AnnotationDisplay< A > annotationDisplay )
	{
		this.name = annotationDisplay.name;

		// Note that even if there are multiple images shown,
		// they must have all the same display settings
		// (this is the definition of them being displayed together)
		// Thus we can fetch the display settings from any of the
		// SourceAndConverter
		final SourceAndConverter< ? > sourceAndConverter = annotationDisplay.nameToSourceAndConverter.values().iterator().next();
		this.opacity = OpacityAdjuster.getOpacity( sourceAndConverter );

		this.lut = annotationDisplay.coloringModel.getARGBLutName();

		final ColoringModel< A > wrappedColoringModel = annotationDisplay.coloringModel.getWrappedColoringModel();

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

		this.showScatterPlot = annotationDisplay.scatterPlotView.isVisible();
		this.scatterPlotAxes = annotationDisplay.scatterPlotView.getSelectedColumns();
		this.tables = annotationDisplay.tables;
		List<String> additionalTables = annotationDisplay.tableView.getAdditionalTables();
		if ( additionalTables.size() > 0 ){
			if ( this.tables == null ) {
				this.tables = new ArrayList<>();
			}
			this.tables.addAll( additionalTables );
		}

		this.showTable = annotationDisplay.tableView.getWindow().isVisible();

		final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		this.showAsBoundaries = boundarySource.isShowAsBoundaries();
		this.boundaryThickness = boundarySource.getBoundaryWidth();
	}

	public abstract void mergeColumns( Map< String, List< String > > columns );
}
