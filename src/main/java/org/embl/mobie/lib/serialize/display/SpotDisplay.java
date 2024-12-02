/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.image.SpotAnnotationImage;
import org.embl.mobie.lib.source.AnnotationType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpotDisplay< AR extends AnnotatedRegion > extends AbstractAnnotationDisplay< AR >
{
	// Serialization

	public List< String > sources;

	private Set< String > selectedSpotIds;

	public Double spotRadius;

	// Runtime

	@Override
	public Set< String > selectedAnnotationIds()
	{
		return selectedSpotIds;
	}

	@Override
	public void setSelectedAnnotationIds( Set< String > selectedSpotIds )
	{
		this.selectedSpotIds = selectedSpotIds;
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	// Gson
	public SpotDisplay()
	{
		super();
	}

	public SpotDisplay( String name )
	{
		super( name );
	}

	// Gson
//	public SpotDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, Set< String > selectedSpotIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, boolean showAsBoundaries, float boundaryThickness  )
//	{
//		this.name = name;
//		this.opacity = opacity;
//		this.lut = lut;
//		this.colorByColumn = colorByColumn;
//		this.valueLimits = valueLimits;
//		this.selectedSpotIds = selectedSpotIds;
//		this.showScatterPlot = showScatterPlot;
//		this.scatterPlotAxes = scatterPlotAxes;
//		this.tables = tables;
//		this.showAsBoundaries = showAsBoundaries;
//		this.boundaryThickness = boundaryThickness;
//	}

	// Create a serializable copy
	public SpotDisplay( SpotDisplay< ? extends Annotation > spotDisplay )
	{
		super( spotDisplay );

		// set fields specific to SpotDisplay
		final SourceAndConverter< ? extends AnnotationType< ? extends Annotation > > sourceAndConverter = spotDisplay.sourceAndConverters().get( 0 );

		// spot radius
		final SpotAnnotationImage spotAnnotationImage = ( SpotAnnotationImage ) DataStore.sourceToImage().get( sourceAndConverter );
		this.spotRadius = spotAnnotationImage.getRadius();
	}
}
