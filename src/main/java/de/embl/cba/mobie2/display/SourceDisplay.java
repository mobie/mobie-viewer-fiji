package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.view.ImageViewer;

import java.util.Collections;
import java.util.List;

public class SourceDisplay
{
	private final String name;
	private final List< String > sources;

	public transient List< SourceAndConverter< ? > > sourceAndConverters;
	public transient ImageViewer imageViewer;

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
