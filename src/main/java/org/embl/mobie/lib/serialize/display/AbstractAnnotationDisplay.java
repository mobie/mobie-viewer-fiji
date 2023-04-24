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
package org.embl.mobie.lib.serialize.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.bdv.view.AnnotationSliceView;
import org.embl.mobie.lib.color.AbstractAnnotationColoringModel;
import org.embl.mobie.lib.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.color.NumericAnnotationColoringModel;
import org.embl.mobie.lib.color.OpacityHelper;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.image.AnnotationImage;
import org.embl.mobie.lib.plot.ScatterPlotView;
import org.embl.mobie.lib.select.SelectionModel;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.BoundarySource;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.table.AnnDataHelper;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableView;
import net.imglib2.util.ValuePair;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//
// This class holds all the information that is
// needed to both view and serialise the display.
//
// Note: This could still be compatible with Spots visualisation
//
//
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
	private double boundaryThickness = 1.0F;
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

	public AbstractAnnotationDisplay( String name )
	{
		this();
		this.name = name;
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

	// in addition to the default table chunk
	public List< String > getRequestedTableChunks()
	{
		return additionalTables;
	}

	public boolean showTable()
	{
		return showTable;
	}

	public boolean showAsBoundaries()
	{
		return showAsBoundaries;
	}

	public void showAsBoundaries( boolean showAsBoundaries )
	{
		this.showAsBoundaries = showAsBoundaries;
	}

	public double getBoundaryThickness()
	{
		return boundaryThickness;
	}

	public void setBoundaryThickness( double boundaryThickness )
	{
		this.boundaryThickness = boundaryThickness;
	}

	public int getRandomColorSeed()
	{
		return randomColorSeed;
	}

	public ARGBType getSelectionColor()
	{
		return ColorHelper.getARGBType( selectionColor );
	}

	public double getOpacityNotSelected()
	{
		return opacityNotSelected;
	}


	@Override
	public BlendingMode getBlendingMode()
	{
		return blendingMode != null ? blendingMode : BlendingMode.Alpha;
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
		this.selectionColor = ColorHelper.getString( mobieColoringModel.getSelectionColor() );

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
		this.scatterPlotAxes = annotationDisplay.scatterPlotView.getSettings().selectedColumns;
		this.additionalTables = annotationDisplay.additionalTables;

		final LinkedHashSet< String > loadedTableChunks = annotationDisplay.annData.getTable().getLoadedTableChunks();
		if ( loadedTableChunks.size() > 0 )
		{
			if ( this.additionalTables == null )
				this.additionalTables = new ArrayList<>();

			for ( String chunk : loadedTableChunks )
			{
				//final String fileName = IOHelper.getFileName( chunk );
				this.additionalTables.add( chunk );
			}
		}

		this.showTable = annotationDisplay.tableView.getWindow().isVisible();

		final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		this.showAsBoundaries = boundarySource.showAsBoundaries();
		this.boundaryThickness = boundarySource.getBoundaryWidth();

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
		final List< AnnotationImage< A > > annotationImages = images().stream().map( image -> ( AnnotationImage< A > ) image ).collect( Collectors.toList() );

		annData = AnnDataHelper.concatenate( annotationImages );

		// FIXME This only is a uuidAdaptor, the stl is not needed
		annotationAdapter = new DefaultAnnotationAdapter<>( annData );
	}

	public AnnotationAdapter<A> annotationAdapter()
	{
		return annotationAdapter;
	}
}
