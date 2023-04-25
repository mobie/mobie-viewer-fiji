package org.embl.mobie.lib.table.columns;

import org.embl.mobie.lib.table.ColumnNames;

import java.util.Collection;

public class IlastikSegmentColumnNames implements SegmentColumnNames
{
	public static final String NONE = "None";
	public static final String LABEL_ID = "Label";
	public static final String[] ANCHOR = { "Centroid.X", "Centroid.Y", "Centroid.Z" };
	public static final String[] BB_MIN = { "Box.X.Min", "Box.Y.Min", "Box.Z.Min" };
	public static final String[] BB_MAX = { "Box.X.Max", "Box.Y.Max", "Box.Z.Max" };
	public static final String TIMEPOINT = "Timepoint";

	@Override
	public String labelImageColumn()
	{
		return ColumnNames.LABEL_IMAGE_ID;
	}

	@Override
	public String labelIdColumn()
	{
		return ColumnNames.LABEL_ID;
	}

	@Override
	public String timePointColumn()
	{
		return ColumnNames.TIMEPOINT;
	}

	@Override
	public String[] anchorColumns()
	{
		return new String[]{ ColumnNames.ANCHOR_X, ColumnNames.ANCHOR_Y, ColumnNames.ANCHOR_Z };
	}

	@Override
	public String[] bbMinColumns()
	{
		return new String[]{ ColumnNames.BB_MIN_X, ColumnNames.BB_MIN_Y, ColumnNames.BB_MIN_Z };
	}

	@Override
	public String[] bbMaxColumns()
	{
		return new String[]{ ColumnNames.BB_MAX_X, ColumnNames.BB_MAX_Y, ColumnNames.BB_MAX_Z };
	}

	public static boolean matches( Collection< String > columns )
	{
		return columns.contains( ColumnNames.ANCHOR_X );
	}

}
