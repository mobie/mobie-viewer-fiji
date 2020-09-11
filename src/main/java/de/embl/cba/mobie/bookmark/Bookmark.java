package de.embl.cba.mobie.bookmark;

import de.embl.cba.mobie.image.MutableImageProperties;

import java.util.HashMap;

public class Bookmark
{
	public String name;
	public HashMap< String, MutableImageProperties > layers;
	public double[] view; // ViewerTransform
	public double[] normView; // ViewerTransform, normalised for BDV window size
	public double[] position;
}
