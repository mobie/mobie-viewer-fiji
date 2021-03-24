package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;

import java.util.List;

public class SourceDisplay
{
	public String name;
	public List< String > sources;

	public transient List< SourceAndConverter< ? > > sourceAndConverters;
}
