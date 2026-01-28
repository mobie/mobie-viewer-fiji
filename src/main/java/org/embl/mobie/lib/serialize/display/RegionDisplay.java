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

import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.Annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionDisplay< AR extends AnnotatedRegion > extends AbstractAnnotationDisplay< AR >
{
	// Serialization

	public Map< String, List< String > > sources; // one region annotating several images

	public String tableSource; // region data source

	public Set< String > selectedRegionIds;

	// Other

	private transient Set< Integer > timepoints = new HashSet<>(); // which timepoints to annotate

	private transient String sourceNamesRegex; // optionally parse the source names to create annotations

	private transient boolean boundaryThicknessIsRelative = false; //

	// TODO create an issue to discuss this
	private transient double relativeDilation = 0.05; // dilation of the regions, useful for rendering as outside boundaries

	private boolean overlap; // Signifies whether some of the regions can overlap with each other.

	public boolean boundaryThicknessIsRelative()
	{
		return boundaryThicknessIsRelative;
	}

	public void boundaryThicknessIsRelative( boolean boundaryThicknessIsRelative )
	{
		this.boundaryThicknessIsRelative = boundaryThicknessIsRelative;
	}

	public double getRelativeDilation()
	{
		return relativeDilation;
	}

	public void setRelativeDilation( double relativeDilation )
	{
		this.relativeDilation = relativeDilation;
	}

	// Used by Gson
	public RegionDisplay()
	{
		super();
	}

	public RegionDisplay( String name )
	{
		super( name );
	}

	// Project creator serialization (currently not used)
//	public RegionDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, Set< String > selectedRegionIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, boolean showAsBoundaries, float boundaryThickness  )
//	{
//		this.name = name;
//		this.opacity = opacity;
//		this.sources = sources;
//		this.lut = lut;
//		this.colorByColumn = colorByColumn;
//		this.valueLimits = valueLimits;
//		this.selectedRegionIds = selectedRegionIds;
//		this.showScatterPlot = showScatterPlot;
//		this.scatterPlotAxes = scatterPlotAxes;
//		this.tables = tables;
//		this.showAsBoundaries = showAsBoundaries;
//		this.boundaryThickness = boundaryThickness;
//	}

	// Create a serializable copy
	public RegionDisplay( RegionDisplay< ? extends Annotation > regionDisplay )
	{
		super( regionDisplay );

		// set fields specific to RegionDisplay
		//
		this.sources = new HashMap< String, List< String > >();
		this.sources.putAll( regionDisplay.sources );
		this.tableSource = regionDisplay.tableSource;
	}

	@Override
	public Set< String > selectedAnnotationIds()
	{
		return selectedRegionIds;
	}

	@Override
	public void setSelectedAnnotationIds( Set< String > selectedAnnotationIds )
	{
		this.selectedRegionIds = selectedAnnotationIds;
	}

	@Override
	public List< String > getSources()
	{
		// there is only one image source that can be displayed,
		// namely the {@code AnnotatedLabelImage} that was created.
		final ArrayList< String > sources = new ArrayList<>();
		sources.add( getName() );
		return sources;
	}

	public Set< Integer > timepoints()
	{
		return timepoints;
	}

	public String getSourceNamesRegex()
	{
		return sourceNamesRegex;
	}

	public void setSourceNamesRegex( String sourceNamesRegex )
	{
		this.sourceNamesRegex = sourceNamesRegex;
	}

	public boolean overlap()
	{
		return overlap;
	}

	public void setOverlap( boolean overlap )
	{
		this.overlap = overlap;
	}
}
