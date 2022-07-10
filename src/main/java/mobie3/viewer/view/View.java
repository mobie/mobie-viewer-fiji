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
package mobie3.viewer.view;

import mobie3.viewer.display.Display;
import mobie3.viewer.transform.Transformation;
import mobie3.viewer.transform.ViewerTransform;

import java.util.ArrayList;
import java.util.List;

public class View
{
	private String uiSelectionGroup;
	private List< Display > displays;
	private List< Transformation > sourceTransforms;
	private ViewerTransform viewerTransform;
	private boolean isExclusive = false;
	private String name;

	public View( String uiSelectionGroup, List< Display > displays,
				 List< Transformation > sourceTransforms, ViewerTransform viewerTransform, boolean isExclusive ) {
		this.uiSelectionGroup = uiSelectionGroup;
		this.displays = displays;
		this.sourceTransforms = sourceTransforms;
		this.viewerTransform = viewerTransform;
		this.isExclusive = isExclusive;
	}

	public View( String uiSelectionGroup, List< Display > displays,
				 List< Transformation > sourceTransforms, boolean isExclusive ) {
		this.uiSelectionGroup = uiSelectionGroup;
		this.displays = displays;
		this.sourceTransforms = sourceTransforms;
		this.isExclusive = isExclusive;
	}

	public boolean isExclusive()
	{
		return isExclusive;
	}

	public List< Transformation > getImageTransformers()
	{
		if ( sourceTransforms == null )
			return new ArrayList<>();
		else
			return sourceTransforms;
	}

	public List< Display > getDisplays()
	{
		if ( displays == null )
			return new ArrayList<>();
		else
			return displays;
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
