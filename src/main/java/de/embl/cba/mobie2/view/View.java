package de.embl.cba.mobie2.view;

import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.transform.SourceTransformerSupplier;

import java.util.List;

public class View
{
	private String uiSelectionGroup;
	private List< SourceDisplaySupplier > sourceDisplays;
	private List< SourceTransformerSupplier > sourceTransforms;
	private boolean isExclusive = false;

	public boolean isExclusive()
	{
		return isExclusive;
	}

	public List< SourceTransformerSupplier > getSourceTransforms()
	{
		return sourceTransforms;
	}

	public List< SourceDisplaySupplier > getSourceDisplays()
	{
		return sourceDisplays;
	}

	public String getUiSelectionGroup()
	{
		return uiSelectionGroup;
	}
}