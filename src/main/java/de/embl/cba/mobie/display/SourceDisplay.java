package de.embl.cba.mobie.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.transform.SourceTransformer;
import de.embl.cba.mobie.bdv.view.SliceViewer;

import java.util.List;

public abstract class SourceDisplay
{
	// Serialization
	protected String name;
	protected double opacity = 1.0;
	protected boolean visible = true;

	// Runtime
	public transient List< SourceAndConverter< ? > > sourceAndConverters;
	public transient List< SourceTransformer > sourceTransformers;
	public transient SliceViewer sliceViewer;

	public String getName()
	{
		return name;
	}
	public double getOpacity()
	{
		return opacity;
	}
	public boolean isVisible()
	{
		return visible;
	}

}
