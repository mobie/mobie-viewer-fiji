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
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionDisplay< AR extends AnnotatedRegion > extends AnnotationDisplay< AR >
{
	// Serialization

	// annotationId to image sources
	// one annotationId can annotate several images
	public Map< String, List< String > > sources;

	// table with each row corresponding to one
	// annotationId
	public Map< TableDataFormat, StorageLocation > tableData;

	public Set< String > selectedRegionIds;

	// Runtime


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
	public void createAnnData()
	{

	}

	public String getTableDataFolder( TableDataFormat tableDataFormat )
	{
		return tableData.get( tableDataFormat ).relativePath;
	}

	@Override
	public List< String > getSources()
	{
		final ArrayList< String > sources = new ArrayList<>();
		sources.add( getName() );
		return sources;
	}

	// Needed for Gson
	public RegionDisplay() {}

	// Needed for Gson
	public RegionDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, Set< String > selectedRegionIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables, boolean showAsBoundaries, float boundaryThickness  )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedRegionIds = selectedRegionIds;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
		this.showAsBoundaries = showAsBoundaries;
		this.boundaryThickness = boundaryThickness;
	}

	// Create a serializable copy
	public RegionDisplay( RegionDisplay< ? extends Annotation > regionDisplay )
	{
		// set properties common to all AnnotationDisplays
		//
		setAnnotationDisplayProperties( regionDisplay );

		// set properties specific to RegionDisplay
		//
		this.sources = new HashMap<>();
		this.sources.putAll( regionDisplay.sources );

		this.tableData = new HashMap<>();
		this.tableData.putAll( regionDisplay.tableData );
	}
}
