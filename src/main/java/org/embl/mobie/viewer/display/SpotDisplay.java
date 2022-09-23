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

import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.annotation.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpotDisplay< AR extends AnnotatedRegion > extends AnnotationDisplay< AR >
{
	// Serialization

	// table with each row corresponding to one spot
	public Set< String > selectedSpotIds;

	// Runtime

	@Override
	public Set< String > selectedAnnotationIds()
	{
		return selectedSpotIds;
	}

	@Override
	public void setSelectedAnnotationIds( Set< String > selectedAnnotationIds )
	{
		this.selectedSpotIds = selectedAnnotationIds;
	}

	@Override
	public List< String > getSources()
	{
		// there is only one source that can be displayed,
		// namely the SpotLabelImage that represents the spots
		final ArrayList< String > sources = new ArrayList<>();
		sources.add( getName() );
		return sources;
	}

	// Needed for Gson
	public SpotDisplay() {}

	// Needed for Gson
	public SpotDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, Set< String > selectedSpotIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, boolean showAsBoundaries, float boundaryThickness  )
	{
		this.name = name;
		this.opacity = opacity;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedSpotIds = selectedSpotIds;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
		this.showAsBoundaries = showAsBoundaries;
		this.boundaryThickness = boundaryThickness;
	}

	// Create a serializable copy
	public SpotDisplay( SpotDisplay< ? extends Annotation > spotDisplay )
	{
		// set properties common to all AnnotationDisplays
		//
		setAnnotationDisplayProperties( spotDisplay );
	}
}
