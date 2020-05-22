package de.embl.cba.platynereis.image;

import de.embl.cba.platynereis.bookmark.Bookmark;

import java.util.ArrayList;

public class MutableImageProperties
{
	public String color;
	public String colorByColumn;
	public double[] contrastLimits;
	public ArrayList< String > tables;
	public ArrayList< Double > selectedLabelIds;
	public boolean showSelectedSegmentsIn3d;
	public boolean showImageIn3d;
}
