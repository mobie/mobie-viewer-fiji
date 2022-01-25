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
	private String description;

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

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }
}