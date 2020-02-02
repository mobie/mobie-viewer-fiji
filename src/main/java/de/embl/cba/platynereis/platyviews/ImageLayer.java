package de.embl.cba.platynereis.platyviews;

import java.util.ArrayList;

public class ImageLayer
{
	public String imageSourceName;
	public String color;
	public double colorLutMin;
	public double colorLutMax;
	public ArrayList< Integer > selectedIds = new ArrayList<>(  );
}
