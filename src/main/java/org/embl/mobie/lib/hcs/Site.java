/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.hcs;

import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.toml.TPosition;
import org.embl.mobie.io.toml.ZPosition;
import org.embl.mobie.lib.io.StorageLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public class Site extends StorageLocation
{
	private final String name;
	private int[] dimensions;
	private Map< TPosition, Map< ZPosition, String > > paths = new LinkedHashMap();
	private VoxelDimensions voxelDimensions;
	private ImageDataFormat imageDataFormat;

	public Site( String name, ImageDataFormat imageDataFormat )
	{
		this.name = name;
		this.imageDataFormat = imageDataFormat;
		this.channel = 0;
	}

	public String getName()
	{
		return name;
	}

	public int[] getDimensions()
	{
		return dimensions;
	}

	public void setDimensions( int[] dimensions )
	{
		this.dimensions = dimensions;
	}

	public void addPath( TPosition t, ZPosition z, String path )
	{
		if ( ! paths.containsKey( t ) )
		{
			paths.put( t, new LinkedHashMap<>() );
		}

		paths.get( t ).put( z, path );
	}

	public void addPath( String t, String z, String path )
	{
		final TPosition tPosition = new TPosition( t );
		final ZPosition zPosition = new ZPosition( z );
		addPath( tPosition, zPosition, path );
	}

	public Map< TPosition, Map< ZPosition, String > > getPaths()
	{
		return paths;
	}

	public void setVoxelDimensions( VoxelDimensions voxelDimensions )
	{
		this.voxelDimensions = voxelDimensions;
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	public ImageDataFormat getImageDataFormat()
	{
		return imageDataFormat;
	}
}
