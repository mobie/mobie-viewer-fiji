package org.embl.mobie.viewer.table.columns;

import java.util.Collection;

public class SkimageSegmentColumnNames implements SegmentColumnNames
{
	private static final String NONE = "None";
	private static final String LABEL_ID = "label";
	private static final String TIMEPOINT = "frame";
	private String[] ANCHOR;
	private String[] BB_MIN;
	private String[] BB_MAX;

	// https://github.com/mobie/mobie-viewer-fiji/issues/935
	// TODO add image calibration?

	public SkimageSegmentColumnNames( Collection< String > columns  )
	{
		this( numDimensions( columns ) );
	}

	public SkimageSegmentColumnNames( int numDimensions )
	{
		ANCHOR = new String[ numDimensions ];
		BB_MIN = new String[ numDimensions ];
		BB_MAX = new String[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			final int i = numDimensions - 1 - d;

			ANCHOR[ d ] = "centroid-" + i;
			BB_MIN[ d ] = "bbox-" + i;
			BB_MAX[ d ] = "bbox-" + ( i + numDimensions );
		}
	}

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
		return columns.contains( "centroid-0" );
	}

	public static int numDimensions( Collection< String > columns )
	{
		return columns.contains( "centroid-2" ) ? 3 : 2;
	}

	public static void main( String[] args )
	{
		final SkimageSegmentColumnNames columnNames2D = new SkimageSegmentColumnNames( 2 );
		final SkimageSegmentColumnNames columnNames3D = new SkimageSegmentColumnNames( 3 );
	}
}
