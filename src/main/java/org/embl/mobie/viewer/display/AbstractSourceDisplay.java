package org.embl.mobie.viewer.display;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.transform.SourceTransformer;
import org.embl.mobie.viewer.bdv.view.SliceViewer;

import java.util.List;
import java.util.Map;

public abstract class AbstractSourceDisplay implements SourceDisplay
{
	// Serialization
	protected String name;
	protected double opacity = 1.0;
	protected boolean visible = true;
	protected BlendingMode blendingMode;

	// Runtime
	public transient Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter;
	public transient SliceViewer sliceViewer;

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	@Override
	public boolean isVisible() { return visible; }

	@Override
	public BlendingMode getBlendingMode()
	{
		return BlendingMode.Sum;
	}
}
