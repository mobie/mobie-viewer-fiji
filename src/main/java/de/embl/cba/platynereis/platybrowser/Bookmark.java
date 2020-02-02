package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.platynereis.platyviews.ImageLayer;

import java.util.ArrayList;

public class Bookmark
{
	public String name;
	public double[] position = null;
	public double[] view = null;
	public ArrayList< ImageLayer > imageLayers = new ArrayList<>();
}
