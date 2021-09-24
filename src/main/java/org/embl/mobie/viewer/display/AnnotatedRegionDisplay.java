package org.embl.mobie.viewer.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.color.MoBIEColoringModel;
import org.embl.mobie.viewer.plot.ScatterPlotViewer;
import org.embl.mobie.viewer.table.TableViewer;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;

import java.util.ArrayList;
import java.util.List;

public abstract class AnnotatedRegionDisplay< T extends TableRow > extends AbstractSourceDisplay
{
	// Serialization
	protected String lut = ColoringLuts.GLASBEY;
	protected String colorByColumn;
	protected Double[] valueLimits = new Double[]{ null, null };
	protected boolean showScatterPlot = false;
	protected String[] scatterPlotAxes = new String[]{ TableColumnNames.ANCHOR_X, TableColumnNames.ANCHOR_Y };
	protected List< String > tables; // tables to display
	protected boolean showTable = true;

	// Runtime
	public transient SelectionModel< T > selectionModel;
	public transient MoBIEColoringModel< T > coloringModel;
	public transient TableViewer< T > tableViewer;
	public transient ScatterPlotViewer< T > scatterPlotViewer;
	public transient List< T > tableRows;

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

	protected void fetchCurrentSettings ( AnnotatedRegionDisplay<T> annotatedRegionDisplay ) {
		this.name = annotatedRegionDisplay.name;

		final SourceAndConverter< ? > sourceAndConverter = annotatedRegionDisplay.sourceNameToSourceAndConverter.values().iterator().next();

		if( sourceAndConverter.getConverter() instanceof LabelConverter)
		{
			this.opacity = ( ( LabelConverter ) sourceAndConverter.getConverter() ).getOpacity();
		}

		this.lut = annotatedRegionDisplay.coloringModel.getARGBLutName();

		final ColoringModel<T> wrappedColoringModel = annotatedRegionDisplay.coloringModel.getWrappedColoringModel();

		if ( wrappedColoringModel instanceof ColumnColoringModel)
		{
			this.colorByColumn = (( ColumnColoringModel ) wrappedColoringModel).getColumnName();
		}

		if ( wrappedColoringModel instanceof NumericColoringModel)
		{
			this.valueLimits = new Double[2];
			NumericColoringModel numericColoringModel = ( NumericColoringModel ) ( wrappedColoringModel );
			valueLimits[0] = numericColoringModel.getMin();
			valueLimits[1] = numericColoringModel.getMax();
		}

		this.showScatterPlot = annotatedRegionDisplay.scatterPlotViewer.isVisible();
		this.scatterPlotAxes = annotatedRegionDisplay.scatterPlotViewer.getSelectedColumns();
		this.tables = annotatedRegionDisplay.tables;
		List<String> additionalTables = annotatedRegionDisplay.tableViewer.getAdditionalTables();
		if ( additionalTables.size() > 0 ){
			if ( this.tables == null ) {
				this.tables = new ArrayList<>();
			}
			this.tables.addAll( additionalTables );
		}

		this.showTable = annotatedRegionDisplay.tableViewer.getWindow().isVisible();
	}

}
