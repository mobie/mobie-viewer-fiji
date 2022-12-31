package org.embl.mobie.viewer.table.columns;

import java.util.Collection;
import java.util.List;

public class MorpholibJSegmentColumnNames implements SegmentColumnNames
{
	public static final String NONE = "None";
	public static final String LABEL_ID = "Label";
	public static final String[] ANCHOR = { "Centroid.X", "Centroid.Y", "Centroid.Z" };
	public static final String[] BB_MIN = { "Box.X.Min", "Box.Y.Min", "Box.Z.Min" };
	public static final String[] BB_MAX = { "Box.X.Max", "Box.Y.Max", "Box.Z.Max" };

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
		return NONE;
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
		return columns.contains( ANCHOR[ 0 ] );
	}
}
