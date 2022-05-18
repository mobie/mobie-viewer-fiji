package org.embl.mobie.viewer.display;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.color.MoBIEColoringModel;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.plot.ScatterPlotViewer;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.table.TableViewer;
import de.embl.cba.tables.color.ColoringLuts;
import org.embl.mobie.viewer.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;

import java.util.ArrayList;
import java.util.List;

public abstract class AnnotationDisplay< T extends TableRow > extends AbstractSourceDisplay
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
	protected int randomColorSeed;

	// Fixed
	protected transient final BlendingMode blendingMode = BlendingMode.SumOccluding;

	// Runtime
	public transient SelectionModel< T > selectionModel;
	public transient MoBIEColoringModel< T > coloringModel;
	public transient TableViewer< T > tableViewer;
	public transient ScatterPlotViewer< T > scatterPlotViewer;
	public transient List< T > tableRows;

	// Should be overwritten by child classes
	public AnnotationSliceView< ? > getSliceView()
	{
		return null;
	}

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

	protected void fetchCurrentSettings( AnnotationDisplay<T> annotationDisplay )
	{
		this.name = annotationDisplay.name;

		final SourceAndConverter< ? > sourceAndConverter = annotationDisplay.sourceNameToSourceAndConverter.values().iterator().next();

		if( sourceAndConverter.getConverter() instanceof OpacityAdjuster )
		{
			final OpacityAdjuster opacityAdjuster = ( OpacityAdjuster ) sourceAndConverter.getConverter();
			this.opacity = opacityAdjuster.getOpacity();
		}

		this.lut = annotationDisplay.coloringModel.getARGBLutName();

		final ColoringModel<T> wrappedColoringModel = annotationDisplay.coloringModel.getWrappedColoringModel();

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

		if ( sourceAndConverter.getSpimSource() instanceof TransformedSource )
		{
			final Source< ? > source = ( ( TransformedSource< ? > ) sourceAndConverter.getSpimSource() ).getWrappedSource();
			if ( source instanceof LabelSource )
			{
				final LabelSource labelSource = ( LabelSource ) source;
				this.showAsBoundaries = labelSource.isShowAsBoundaries();
				this.boundaryThickness = labelSource.getBoundaryWidth();
			}
		}
	}
}
