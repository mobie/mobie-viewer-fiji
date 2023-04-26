package org.embl.mobie.lib.table.columns;

import org.embl.mobie.lib.table.ColumnNames;

import java.util.Collection;

public class IlastikSegmentColumnNames implements SegmentColumnNames
{
	public static final String NONE = "None";
	public static final String LABEL_ID = "labelimageId";
	public static final String[] ANCHOR = { "Object_Center_0", "Object_Center_1", "Object_Center_2" };
	public static final String[] BB_MIN = { "Bounding_Box_Minimum_0", "Bounding_Box_Minimum_1", "Bounding_Box_Minimum_2" };
	public static final String[] BB_MAX = { "Bounding_Box_Maximum_0", "Bounding_Box_Maximum_1", "Bounding_Box_Maximum_2" };
	public static final String TIMEPOINT = "frame";

	@Override
	public String labelImageColumn()
	{
		return NONE;
	}

	@Override
	public String labelIdColumn()
	{
		return LABEL_ID;
	}

	@Override
	public String timePointColumn()
	{
		return TIMEPOINT;
	}

	@Override
	public String[] anchorColumns()
	{
		return ANCHOR;
	}

	@Override
	public String[] bbMinColumns()
	{
		return BB_MIN;
	}

	@Override
	public String[] bbMaxColumns()
	{
		return BB_MAX;
	}

	public static boolean matches( Collection< String > columns )
	{
		return columns.contains( LABEL_ID );
	}

}
