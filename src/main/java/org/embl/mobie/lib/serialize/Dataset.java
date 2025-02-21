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
package org.embl.mobie.lib.serialize;

import ij.IJ;
import org.embl.mobie.lib.transform.viewer.PositionViewerTransform;
import org.embl.mobie.lib.transform.viewer.ViewerTransform;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Dataset
{
	// Serialisation
	private boolean is2D = false;
	private ViewerTransform defaultLocation = new PositionViewerTransform( new double[]{0,0,0}, 0 );
	private Map< String, DataSource > sources = new HashMap<>();
	private Map< String, View > views = new LinkedHashMap<>();

	// Runtime
	private String name;

	public Dataset() { }

	public Dataset( String name ) {
		this.name = name;
	}

	public Dataset( boolean is2D, ViewerTransform defaultLocation, Map< String, DataSource > sources, Map< String, View > views )
	{
		this.is2D = is2D;
		this.defaultLocation = defaultLocation;
		this.sources = sources;
		this.views = views;
	}

	public Map< String, View > views()
	{
		for ( String name : views.keySet() )
			views.get( name ).setName( name );

		return views;
	}

	public Map< String, DataSource > sources()
	{
		return sources;
	}

	public void putDataSource( DataSource dataSource )
	{
		if ( sources.containsKey( dataSource.getName() ) )
			IJ.log("[WARN] " + dataSource.getName() + " has been added already and is now replaced; choose unique names for your data!");
		sources.put( dataSource.getName(), dataSource );
	}

	public boolean is2D()
	{
		return is2D;
	}

	public void is2D( boolean is2D )
	{
		this.is2D = is2D;
	}

	public ViewerTransform getDefaultLocation()
	{
		return defaultLocation;
	}

	public String getName()
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}
}
