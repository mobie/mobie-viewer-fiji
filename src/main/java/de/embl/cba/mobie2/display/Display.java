package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.mobie2.view.SliceViewer;
import de.embl.cba.tables.imagesegment.ImageSegment;

import java.util.Collections;
import java.util.List;

public class Display
{
	// Serialization
	private String name;
	private double opacity;
	private List< String > sources;

	// Runtime
	public transient List< SourceAndConverter< ? > > sourceAndConverters;
	public transient List< SourceTransformer > sourceTransformers;
	public transient SliceViewer sliceViewer;

	public String getName()
	{
		return name;
	}

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}
	public double getOpacity()
	{
		return opacity;
	}

	public List< SourceAndConverter< ? > > getSourceAndConverters()
	{
		return sourceAndConverters;
	}

	public List< SourceTransformer > getSourceTransformers()
	{
		return sourceTransformers;
	}

	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}
}
