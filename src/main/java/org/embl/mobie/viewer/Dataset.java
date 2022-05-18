package org.embl.mobie.viewer;

import org.embl.mobie.viewer.source.SourceSupplier;
import org.embl.mobie.viewer.transform.ViewerTransform;
import org.embl.mobie.viewer.view.View;

import java.util.Map;

public class Dataset
{
	public boolean is2D = false;
	public ViewerTransform defaultLocation;
	public int timepoints = 1;
	public Map< String, SourceSupplier> sources;
	public Map< String, View> views;
}
