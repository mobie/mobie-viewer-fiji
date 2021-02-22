package de.embl.cba.mobie.image;

import java.util.ArrayList;

public class MutableImageProperties
{
	public String color;
	public String colorByColumn;
	public double[] contrastLimits;
	public double[] valueLimits;
	public double resolution3dView;
	public ArrayList< String > tables;
	public ArrayList< Double > selectedLabelIds;
	public boolean showSelectedSegmentsIn3d;
	public boolean showImageIn3d;
	public double[] addedTransform;
}
