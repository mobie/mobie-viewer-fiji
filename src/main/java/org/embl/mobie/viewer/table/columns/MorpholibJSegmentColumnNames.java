package org.embl.mobie.viewer.table.columns;

public class MorpholibJSegmentColumnNames implements SegmentColumnNames
{
	public static final String LABEL = "Label";
	public static final String CENTROID_X = "Centroid.X";
	public static final String CENTROID_Y = "Centroid.Y";
	public static final String CENTROID_Z = "Centroid.Z";
	public static final String NONE = "None";
	public static final String MEAN_BREADTH = "MeanBreadth";

	@Override
	public String labelImageColumn()
	{
		return NONE;
	}

	@Override
	public String labelIdColumn()
	{
		return LABEL;
	}

	@Override
	public String timePointColumn()
	{
		return NONE;
	}

	@Override
	public String[] anchorColumns()
	{
		return new String[]{ CENTROID_X, CENTROID_Y, CENTROID_Z };
	}

	@Override
	public String[] bbMinColumns()
	{
		return new String[]{ NONE, NONE, NONE };
	}

	@Override
	public String[] bbMaxColumns()
	{
		return new String[]{ NONE, NONE, NONE };
	}
}
