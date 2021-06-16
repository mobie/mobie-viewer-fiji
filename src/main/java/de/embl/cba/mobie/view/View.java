package de.embl.cba.mobie.view;

import de.embl.cba.mobie.display.SourceDisplay;
import de.embl.cba.mobie.transform.SourceTransformer;
import de.embl.cba.mobie.transform.ViewerTransform;

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
		return sourceTransforms;
	}

	public List< SourceDisplay > getSourceDisplays()
	{
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
}