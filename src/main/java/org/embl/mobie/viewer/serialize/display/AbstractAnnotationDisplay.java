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
package org.embl.mobie.viewer.serialize.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.annotation.AnnotationAdapter;
import org.embl.mobie.viewer.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.bdv.view.AnnotationSliceView;
import org.embl.mobie.viewer.color.AbstractAnnotationColoringModel;
import org.embl.mobie.viewer.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.viewer.color.ColorHelper;
import org.embl.mobie.viewer.color.ColoringModel;
import org.embl.mobie.viewer.color.MobieColoringModel;
import org.embl.mobie.viewer.color.NumericAnnotationColoringModel;
import org.embl.mobie.viewer.color.OpacityHelper;
import org.embl.mobie.viewer.color.lut.LUTs;
import org.embl.mobie.viewer.image.AnnotationImage;
import org.embl.mobie.viewer.plot.ScatterPlotView;
import org.embl.mobie.viewer.select.SelectionModel;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.BoundarySource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.table.AnnDataHelper;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.table.TableView;
import net.imglib2.util.ValuePair;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class holds all the information that is
 * needed to both view it and serialise the display.
 *
 * Note: This could still be compatible with Spots visualisation
 *
 * @param <A>
 */
public abstract class AbstractAnnotationDisplay< A extends Annotation > extends AbstractDisplay< AnnotationType< A > >
{
	// Serialization
	protected String lut = LUTs.GLASBEY;
	protected String colorByColumn;
	protected Double[] valueLimits;
	protected boolean showScatterPlot = false;
	protected String[] scatterPlotAxes = new String[]{ ColumnNames.ANCHOR_X, ColumnNames.ANCHOR_Y };
	protected List< String > additionalTables;
	protected boolean showTable = true;
	protected boolean showAsBoundaries = false;
	protected float boundaryThickness = 1.0F;
	protected int randomColorSeed = 42;
	protected String selectionColor = null;
	protected double opacityNotSelected = 0.15;

	// Runtime
	public transient SelectionModel< A > selectionModel;
	public transient MobieColoringModel< A > coloringModel;
	public transient AnnotationAdapter< A > annotationAdapter;
	public transient TableView< A > tableView;
	public transient ScatterPlotView< A > scatterPlotView;
	public transient AnnotationSliceView< A > sliceView;
	protected transient AnnData< A > annData;

	// Methods

	// Used by Gson deserialization
	public AbstractAnnotationDisplay()
	{
		blendingMode = BlendingMode.Alpha;
		opacity = 0.5;
	}

	// Use this for serialization
	public AbstractAnnotationDisplay( AbstractAnnotationDisplay< ? extends Annotation > annotationDisplay )
	{
		setSerializableFields( annotationDisplay );
	}

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
		return new ValuePair<>( valueLimits[ 0 ], valueLimits[ 1 ] );
	}

	public boolean showScatterPlot()
	{
		return showScatterPlot;
	}

	public String[] getScatterPlotAxes()
	{
		return scatterPlotAxes;
	}

	// table = chunk of columns
	public List< String > getAdditionalTables()
	{
		return additionalTables;
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

	public int getRandomColorSeed()
	{
		return randomColorSeed;
	}

	public ARGBType getSelectionColor()
	{
		return ColorHelper.getArgbType( selectionColor );
	}

	public double getOpacityNotSelected()
	{
		return opacityNotSelected;
	}

	private void setSerializableFields( AbstractAnnotationDisplay< ? extends Annotation > annotationDisplay )
	{
		this.name = annotationDisplay.name;

		if ( annotationDisplay.sliceView != null )
			this.visible = annotationDisplay.sliceView.isVisible();

		// Note that even if there are multiple images shown,
		// they must have all the same display settings
		// (this is the definition of them being displayed together).
		// One can therefore fetch the display settings from any of the
		// SourceAndConverter.
		final SourceAndConverter< ? > sourceAndConverter = annotationDisplay.sourceAndConverters().get( 0 );

		// TODO: get opacity from coloring model instead of sac...
		this.opacity = OpacityHelper.getOpacity( sourceAndConverter.getConverter() );

		this.blendingMode = null; // default is Alpha so we don't serialise it
		// this.blendingMode = ( BlendingMode ) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, BlendingMode.class.getName() );

		final MobieColoringModel< ? extends Annotation > mobieColoringModel = annotationDisplay.coloringModel;

		this.opacityNotSelected = mobieColoringModel.getOpacityNotSelected();
		this.selectionColor = ColorHelper.toString( mobieColoringModel.getSelectionColor() );

		final ColoringModel< ? extends Annotation > coloringModel = mobieColoringModel.getWrappedColoringModel();

		if ( coloringModel instanceof AbstractAnnotationColoringModel )
		{
			final AbstractAnnotationColoringModel annotationColoringModel = ( AbstractAnnotationColoringModel ) coloringModel;
			this.lut = annotationColoringModel.getLut().getName();
			this.colorByColumn = annotationColoringModel.getColumnName();
		}

		if ( coloringModel instanceof NumericAnnotationColoringModel )
		{
			this.valueLimits = new Double[2];
			this.valueLimits[0] = ( ( NumericAnnotationColoringModel ) coloringModel ).getMin();
			this.valueLimits[1] = ( ( NumericAnnotationColoringModel ) coloringModel ).getMax();
		}

		if ( coloringModel instanceof CategoricalAnnotationColoringModel )
			this.randomColorSeed = ( ( CategoricalAnnotationColoringModel ) coloringModel ).getRandomSeed();

		this.showScatterPlot = annotationDisplay.scatterPlotView.isVisible();
		this.scatterPlotAxes = annotationDisplay.scatterPlotView.getSelectedColumns();
		this.additionalTables = annotationDisplay.additionalTables;

		final LinkedHashSet< String > additionalTablePaths = annotationDisplay.annData.getTable().getAdditionalTablePaths();
		if ( additionalTablePaths.size() > 0 )
		{
			if ( this.additionalTables == null )
				this.additionalTables = new ArrayList<>();

			for ( String path : additionalTablePaths )
			{
				final String fileName = MoBIEHelper.getFileName( path );
				this.additionalTables.add( fileName );
			}
		}

		this.showTable = annotationDisplay.tableView.getWindow().isVisible();

		final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		this.showAsBoundaries = boundarySource.isShowAsBoundaries();
		this.boundaryThickness = boundarySource.getCalibratedBoundaryWidth();

		final Set< ? extends Annotation > selectedAnnotations = annotationDisplay.selectionModel.getSelected();
		if (selectedAnnotations != null)
			setSelectedAnnotationIds( selectedAnnotations.stream().map( a -> a.uuid() ).collect( Collectors.toSet() ) );
	}

	public AnnData< A > getAnnData()
	{
		return annData;
	}

	// fetch annData from the annotated images
	// the main use is to concatenate the tables
	// in case this display shows several images
	public void initAnnData()
	{
		final List< AnnotationImage< A > > annotationImages = getImages().stream().map( image -> ( AnnotationImage< A > ) image ).collect( Collectors.toList() );

		annData = AnnDataHelper.concatenate( annotationImages );

		// FIXME This only is a uuidAdaptor, the stl is not needed
		annotationAdapter = new DefaultAnnotationAdapter<>( annData );
	}

	public AnnotationAdapter<A> annotationAdapter()
	{
		return annotationAdapter;
	}
}
