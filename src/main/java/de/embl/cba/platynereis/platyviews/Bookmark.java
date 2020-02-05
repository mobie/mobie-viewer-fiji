package de.embl.cba.platynereis.platyviews;

import de.embl.cba.bdv.utils.sources.Metadata;

import java.util.ArrayList;

public class Bookmark
{
	public String name;
	public ArrayList< Double > position = null;
	public ArrayList< Double > transform = null;
	public ArrayList< Metadata > imageLayers = new ArrayList<>();
}
