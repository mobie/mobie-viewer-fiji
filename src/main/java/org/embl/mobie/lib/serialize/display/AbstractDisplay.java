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
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.bdv.view.SliceViewer;
import org.embl.mobie.lib.bvb.BigVolumeBrowserMoBIE;
import org.embl.mobie.lib.image.Image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractDisplay< T > implements Display< T >
{
	// Serialization
	protected String name;
	protected double opacity = 1.0;
	protected boolean visible = true;
	protected BlendingMode blendingMode; // Do not set default to avoid serialisation for AnnotationDisplays

	// Runtime
	private final transient List< Image< T > > images = new ArrayList<>();
	public transient SliceViewer sliceViewer;
	public transient BigVolumeBrowserMoBIE bigVolumeBrowser;

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	@Override
	public boolean isVisible() { return visible; }

	@Override
	public BlendingMode getBlendingMode()
	{
		return blendingMode != null ? blendingMode : BlendingMode.Sum;
	}

	@Override
	public List< Image< T > > images()
	{
		return images;
	}

	@Override
	public List< SourceAndConverter< T > > sourceAndConverters()
	{
		// returns an unmodifiableList
		return DataStore.getSourceAndConverters( ( Collection ) images );
	}

	public void setOpacity( double opacity )
	{
		this.opacity = opacity;
	}

	public void setBlendingMode( BlendingMode blendingMode )
	{
		this.blendingMode = blendingMode;
	}
}
