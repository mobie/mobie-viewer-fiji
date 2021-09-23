package org.embl.mobie.viewer.display;

import bdv.viewer.SourceAndConverter;
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
    private String description;

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

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

	// Runtime
	public transient Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter;
	public transient List<SourceTransformer> sourceTransformers;
	public transient SliceViewer sliceViewer;
}
