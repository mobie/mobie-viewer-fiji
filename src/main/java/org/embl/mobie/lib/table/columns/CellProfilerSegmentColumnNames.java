/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellProfilerSegmentColumnNames implements SegmentColumnNames
{
	private static final String NONE = "None";
	private final String LABEL_ID = "ObjectNumber";

	// TODO: we could also support Location_X here
	private static final String[] AREA_SHAPE_ANCHOR = { "AreaShape_Center_X", "AreaShape_Center_Y", "AreaShape_Center_Z" };
	private static final String[] LOCATION_ANCHOR = { "Location_X", "Location_Y", "Location_Z" };

	public static final String OBJECT = "Object";

	// TODO: we could also support Location_X here
	public static final Pattern SINGLE_OBJECT_AREA_SHAPE_ANCHOR_PATTERN = Pattern.compile( "AreaShape_Center_X" );
	public static final Pattern SINGLE_OBJECT_LOCATION_ANCHOR_PATTERN = Pattern.compile( "Location_X" );
	public static final Pattern MULTI_OBJECT_AREA_SHAPE_ANCHOR_PATTERN = Pattern.compile( "(?<" + OBJECT + ">.*)_AreaShape_Center_X" );
	public static final Pattern MULTI_OBJECT_LOCATION_ANCHOR_PATTERN = Pattern.compile( "(?<" + OBJECT + ">.*)_Location_X" );

	private final String[] anchor;
	private final String labelId;

	public CellProfilerSegmentColumnNames( Collection<String> columnNames )
	{
		final String objectName = getObjectName( columnNames );

		if ( objectName == null )
		{
			// there is only one object type (e.g., only nuclei) in the CellProfiler table
			this.anchor = getSingleObjectAnchorColumns( columnNames );
			this.labelId = LABEL_ID;
		}
		else
		{
			// there are multiple object types (e.g., nuclei and cells) in the CellProfiler table
			this.anchor = getMultiObjectAnchorColumns( columnNames, objectName );
			this.labelId = objectName + "_" + LABEL_ID;
		}
	}

	@NotNull
	private static String[] getMultiObjectAnchorColumns( Collection< String > columns, String objectName )
	{
		String[] anchor = new String[ 3 ];

		for ( String column : columns )
		{
			if( MULTI_OBJECT_AREA_SHAPE_ANCHOR_PATTERN.matcher( column ).matches() )
			{
				for ( int d = 0; d < 3; d++ )
				{
					anchor[ d ] = objectName + "_" + AREA_SHAPE_ANCHOR[ d ];
				}
				return anchor;
			}

			if( MULTI_OBJECT_LOCATION_ANCHOR_PATTERN.matcher( column ).matches() )
			{
				for ( int d = 0; d < 3; d++ )
				{
					anchor[ d ] = objectName + "_" + LOCATION_ANCHOR[ d ];
				}
				return anchor;
			}
		}

		return null; // Should not happen
	}

	@Override
	public String labelImageColumn()
	{
		return NONE;
	}

	@Override
	public String labelIdColumn()
	{
		return labelId;
	}

	@Override
	public String timePointColumn()
	{
		return NONE;
	}

	@Override
	public String[] anchorColumns()
	{
		return anchor;
	}

	@Override
	public String[] bbMinColumns()
	{
		return new String[]{ NONE };
	}

	@Override
	public String[] bbMaxColumns()
	{
		return new String[]{ NONE };
	}

	public static boolean matches( Collection< String > columns )
	{
		for ( String column : columns )
		{
			if( SINGLE_OBJECT_LOCATION_ANCHOR_PATTERN.matcher( column ).matches() )
				return true;

			if( SINGLE_OBJECT_AREA_SHAPE_ANCHOR_PATTERN.matcher( column ).matches() )
				return true;

			if( MULTI_OBJECT_LOCATION_ANCHOR_PATTERN.matcher( column ).matches() )
				return true;

			if( MULTI_OBJECT_AREA_SHAPE_ANCHOR_PATTERN.matcher( column ).matches() )
				return true;
		}

		return false;
	}

	private static String[] getSingleObjectAnchorColumns( Collection< String > columns )
	{
		for ( String column : columns )
		{
			if( SINGLE_OBJECT_LOCATION_ANCHOR_PATTERN.matcher( column ).matches() )
				return LOCATION_ANCHOR;

			if( SINGLE_OBJECT_AREA_SHAPE_ANCHOR_PATTERN.matcher( column ).matches() )
				return AREA_SHAPE_ANCHOR;
		}

		return null;
	}

	private static String getObjectName( Collection< String > columns )
	{
		for ( String column : columns )
		{
			if( SINGLE_OBJECT_AREA_SHAPE_ANCHOR_PATTERN.matcher( column ).matches() )
				return null;

            Matcher matcher = MULTI_OBJECT_AREA_SHAPE_ANCHOR_PATTERN.matcher( column );
			if( matcher.matches() )
				return matcher.group( OBJECT );

			matcher = MULTI_OBJECT_LOCATION_ANCHOR_PATTERN.matcher( column );
			if( matcher.matches() )
				return matcher.group( OBJECT );
		}

		return null;
	}

}
