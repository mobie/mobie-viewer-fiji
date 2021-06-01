package de.embl.cba.mobie;

import de.embl.cba.mobie.source.SourceSupplier;
import de.embl.cba.mobie.view.View;

import java.util.Map;

public class Dataset
{
	public boolean is2D = false;
	public int timepoints = 1;
	public Map< String, SourceSupplier > sources;
	public Map< String, View > views;
}
