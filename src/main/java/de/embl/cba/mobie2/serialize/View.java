package de.embl.cba.mobie2.serialize;

import de.embl.cba.mobie2.display.SourceDisplaySupplier;
import de.embl.cba.mobie2.transform.SourceTransformerSupplier;

import java.util.List;

public class View
{
	public String uiSelectionGroup;
	public List< SourceDisplaySupplier > sourceDisplays;
	public List< SourceTransformerSupplier > sourceTransforms;
}