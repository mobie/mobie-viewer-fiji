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
package org.embl.mobie.viewer.view;

import org.embl.mobie.viewer.display.SourceDisplay;
import org.embl.mobie.viewer.transform.SourceTransformer;
import org.embl.mobie.viewer.transform.ViewerTransform;

import java.util.ArrayList;
import java.util.List;

public class View
{
	private String uiSelectionGroup;
	private List< SourceDisplay > sourceDisplays;
	private List< SourceTransformer > sourceTransforms;
	private ViewerTransform viewerTransform;
	private boolean isExclusive = false;
	private String name;

	public View( String uiSelectionGroup, List< SourceDisplay > sourceDisplays,
				 List< SourceTransformer > sourceTransforms, ViewerTransform viewerTransform, boolean isExclusive ) {
		this.uiSelectionGroup = uiSelectionGroup;
		this.sourceDisplays = sourceDisplays;
		this.sourceTransforms = sourceTransforms;
		this.viewerTransform = viewerTransform;
		this.isExclusive = isExclusive;
	}

	public View( String uiSelectionGroup, List< SourceDisplay > sourceDisplays,
				 List< SourceTransformer > sourceTransforms, boolean isExclusive ) {
		this.uiSelectionGroup = uiSelectionGroup;
		this.sourceDisplays = sourceDisplays;
		this.sourceTransforms = sourceTransforms;
		this.isExclusive = isExclusive;
	}

	public boolean isExclusive()
	{
		return isExclusive;
	}

	public List< SourceTransformer > getSourceTransforms()
	{
		if ( sourceTransforms == null )
			return new ArrayList<>();
		else
			return sourceTransforms;
	}

	public List< SourceDisplay > getSourceDisplays()
	{
		if ( sourceDisplays == null )
			return new ArrayList<>();
		else
			return sourceDisplays;
	}

	public String getUiSelectionGroup()
	{
		return uiSelectionGroup;
	}

	public ViewerTransform getViewerTransform()
	{
		return viewerTransform;
	}

	public String getName()
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
