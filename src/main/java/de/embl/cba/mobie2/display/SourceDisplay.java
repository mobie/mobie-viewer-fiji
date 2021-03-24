package de.embl.cba.mobie2.display;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.view.ImageViewer;

import java.util.List;

public class SourceDisplay
{
	public String name;
	public List< String > sources;

	public transient List< SourceAndConverter< ? > > sourceAndConverters;
	public transient ImageViewer imageViewer;
}
