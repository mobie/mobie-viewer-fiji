/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.table.columns;

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
		return columns.contains( LABEL_ID );
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
