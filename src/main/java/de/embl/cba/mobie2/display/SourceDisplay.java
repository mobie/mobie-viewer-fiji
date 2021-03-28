package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.view.ImageViewer;
import de.embl.cba.tables.imagesegment.ImageSegment;

import java.util.Collections;
import java.util.List;

public class SourceDisplay
{
	private final String name;
	private final List< String > sources;

	public transient List< SourceAndConverter< ? > > sourceAndConverters;
	public transient ImageViewer< ? extends ImageSegment > imageViewer;

	public SourceDisplay( String name, List< String > sources )
	{
		this.name = name;
		this.sources = sources;
	}

	public String getName()
	{
		return name;
	}

	public List< String > getSources()
	{
		return Collections.unmodifiableList( sources );
	}
}
