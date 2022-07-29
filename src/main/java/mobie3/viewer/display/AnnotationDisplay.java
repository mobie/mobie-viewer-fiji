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
import mobie3.viewer.annotation.AnnotationProvider;
import mobie3.viewer.bdv.render.BlendingMode;
import mobie3.viewer.bdv.view.AnnotationSliceView;
import mobie3.viewer.color.AbstractAnnotationColoringModel;
import mobie3.viewer.color.CategoricalAnnotationColoringModel;
import mobie3.viewer.color.ColoringModel;
import mobie3.viewer.color.MoBIEColoringModel;
import mobie3.viewer.color.NumericAnnotationColoringModel;
import mobie3.viewer.color.lut.LUTs;
import mobie3.viewer.plot.ScatterPlotView;
import mobie3.viewer.select.SelectionModel;
import mobie3.viewer.source.AnnotatedImage;
import mobie3.viewer.source.AnnotationType;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.SourceHelper;
import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.table.AnnData;
import mobie3.viewer.table.ColumnNames;
import mobie3.viewer.table.ConcatenatedAnnData;
import mobie3.viewer.table.TableView;
import net.imglib2.util.ValuePair;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class holds all the information that is
 * needed to both view it and serialise it.
 *
 * Note: This could still be compatible with Spots visualisation
 *
 * @param <A>
 */
public abstract class AnnotationDisplay< A extends Annotation > extends AbstractDisplay< AnnotationType< A > >
{
	// Serialization
	protected String lut = LUTs.GLASBEY;
	protected String colorByColumn;
	protected Double[] valueLimits;
	protected boolean showScatterPlot = false;
	protected String[] scatterPlotAxes = new String[]{ ColumnNames.ANCHOR_X, ColumnNames.ANCHOR_Y };
	protected List< String > tables; // tables to display
	protected boolean showTable = true;
	protected boolean showAsBoundaries = false;
	protected float boundaryThickness = 1.0F;
	protected int randomColorSeed = 42;

	// Final
	protected transient final BlendingMode blendingMode = BlendingMode.SumOccluding;

	// Runtime
	public transient SelectionModel< A > selectionModel;
	public transient MoBIEColoringModel< A > coloringModel;
	public transient AnnotationProvider< A > annotationProvider;
	public transient TableView< A > tableView;
	public transient ScatterPlotView< A > scatterPlotView;
	public transient AnnotationSliceView< A > sliceView;
	private transient AnnData< A > annData;

	// Methods
	public abstract Set< String > selectedAnnotationIds();

	public abstract void setSelectedAnnotationIds( Set< String > selectedAnnotationIds );

	public String getLut()
	{
		return lut;
	}

	public String getColoringColumnName()
	{
		return colorByColumn;
	}

	public ValuePair< Double, Double > getValueLimits()
	{
		return new ValuePair< Double, Double >( valueLimits[ 0 ], valueLimits[ 1 ] );
	}

	public boolean showScatterPlot()
	{
		return showScatterPlot;
	}

	public String[] getScatterPlotAxes()
	{
		return scatterPlotAxes;
	}

	public List< String > getDisplayedColumnChunks()
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

	protected void setAnnotationDisplayProperties( AnnotationDisplay< ? extends Annotation > annotationDisplay )
	{
		this.name = annotationDisplay.name;

		if ( annotationDisplay.sliceView != null ) {
			this.visible = annotationDisplay.sliceView.isVisible();
		}

		// Note that even if there are multiple images shown,
		// they must have all the same display settings
		// (this is the definition of them being displayed together).
		// One can therefore fetch the display settings from any of the
		// SourceAndConverter.
		final SourceAndConverter< ? > sourceAndConverter = annotationDisplay.nameToSourceAndConverter.values().iterator().next();

		// TODO: get opacity from coloring model instead of sac!
		//   But from which one? Maybe just from MoBIEColoringModel?!
		//this.opacity = OpacityAdjuster.getOpacity( sourceAndConverter );
		final ColoringModel< ? extends Annotation > coloringModel = annotationDisplay.coloringModel.getWrappedColoringModel();

		if ( coloringModel instanceof AbstractAnnotationColoringModel )
		{
			this.lut = ( ( AbstractAnnotationColoringModel ) coloringModel ).getLut().getName();
			this.colorByColumn = ( ( AbstractAnnotationColoringModel ) coloringModel ).getColumnName();
		}

		if ( coloringModel instanceof NumericAnnotationColoringModel )
		{
			this.valueLimits = new Double[2];
			this.valueLimits[0] = ( ( NumericAnnotationColoringModel ) coloringModel ).getMin();
			this.valueLimits[1] = ( ( NumericAnnotationColoringModel ) coloringModel ).getMax();
		}
		if ( coloringModel instanceof CategoricalAnnotationColoringModel )
		{
			this.randomColorSeed = ( ( CategoricalAnnotationColoringModel ) coloringModel ).getRandomSeed();
		}

		this.showScatterPlot = annotationDisplay.scatterPlotView.isVisible();
		this.scatterPlotAxes = annotationDisplay.scatterPlotView.getSelectedColumns();
		this.tables = annotationDisplay.tables;

		final LinkedHashSet< String > loadedColumnPaths = annotationDisplay.annData.getTable().loadedColumnPaths();
		if ( loadedColumnPaths.size() > 0 ){
			if ( this.tables == null ) {
				this.tables = new ArrayList<>();
			}
			this.tables.addAll( loadedColumnPaths );
		}

		this.showTable = annotationDisplay.tableView.getWindow().isVisible();

		final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		this.showAsBoundaries = boundarySource.isShowAsBoundaries();
		this.boundaryThickness = boundarySource.getBoundaryWidth();

		final Set< ? extends Annotation > selectedAnnotations = annotationDisplay.selectionModel.getSelected();
		if (selectedAnnotations != null) {
			setSelectedAnnotationIds( selectedAnnotations.stream().map( a -> a.getId() ).collect( Collectors.toSet() ) );
		}
	}

	public Set< AnnotatedImage< A > > getAnnotatedImages()
	{
		final Set< AnnotatedImage< A > > annotatedImages = getImages().stream().map( image -> ( AnnotatedImage< A > ) image ).collect( Collectors.toSet() );
		return annotatedImages;
	}

	public void initAnnData()
	{
		final Set< AnnData< A > > annDataSet = getAnnotatedImages().stream().map( image -> image.getAnnData() ).collect( Collectors.toSet() );
		annData = new ConcatenatedAnnData( annDataSet );
	}

	public AnnData< A > getAnnData()
	{
		return annData;
	}
}
