package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.view.BdvViewer;
import de.embl.cba.tables.imagesegment.ImageSegment;

import java.util.Collections;
import java.util.List;

public class SourceDisplay
{
	// Serialization
	private boolean isExclusive;
	private String name;
	private double opacity;
	private List< String > sources;

	// Runtime
	public transient List< SourceAndConverter< ? > > sourceAndConverters;
	public transient BdvViewer< ? extends ImageSegment > bdvViewer;

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

	public boolean isExclusive()
	{
		return isExclusive;
	}
}
