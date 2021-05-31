package de.embl.cba.mobie2.view;

import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.transform.BdvLocationSupplier;
import de.embl.cba.mobie2.transform.SourceTransformer;

import java.util.List;

public class View
{
	private String uiSelectionGroup;
	private List< SourceDisplay > sourceDisplays;
	private List< SourceTransformer > sourceTransforms;
	private BdvLocationSupplier viewerTransform;
	private boolean isExclusive = false;

	public View( String uiSelectionGroup, List< SourceDisplay > sourceDisplays,
				 List< SourceTransformer > sourceTransforms, BdvLocationSupplier viewerTransform,
				 boolean isExclusive ) {
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

	public BdvLocationSupplier getViewerTransform()
	{
		return viewerTransform;
	}
}