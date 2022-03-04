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
	private List< SourceTransformer > sourceTransformers;
	private ViewerTransform viewerTransform;
	private boolean isExclusive = false;

	public View( String uiSelectionGroup, List< SourceDisplay > sourceDisplays, List< SourceTransformer > sourceTransformers, ViewerTransform viewerTransform, boolean isExclusive ) {
		this.uiSelectionGroup = uiSelectionGroup;
		this.sourceDisplays = sourceDisplays;
		this.sourceTransformers = sourceTransformers;
		this.viewerTransform = viewerTransform;
		this.isExclusive = isExclusive;
	}

	public View( String uiSelectionGroup, List< SourceDisplay > sourceDisplays, List< SourceTransformer > sourceTransformers, boolean isExclusive ) {
		this.uiSelectionGroup = uiSelectionGroup;
		this.sourceDisplays = sourceDisplays;
		this.sourceTransformers = sourceTransformers;
		this.isExclusive = isExclusive;
	}

	public boolean isExclusive()
	{
		return isExclusive;
	}

	public List< SourceTransformer > getSourceTransformers()
	{
		if ( sourceTransformers == null )
			return new ArrayList<>();
		else
			return sourceTransformers;
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

}