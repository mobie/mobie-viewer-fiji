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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellProfilerSegmentColumnNames implements SegmentColumnNames
{
	private static final String NONE = "None";
	private final String LABEL_ID = "ObjectNumber";
	private static final String[] ANCHOR = { "AreaShape_Center_X", "AreaShape_Center_Y", "AreaShape_Center_z" };

	public static final String OBJECT = "Object";
	public static final Pattern LOCATION_X_PATTERN = Pattern.compile( "(?<" + OBJECT + ">.*)_AreaShape_Center_X" );

	private final String objectName;
	private final String[] anchor;
	private final String labelId;

	public CellProfilerSegmentColumnNames( String objectName )
	{
		this.objectName = objectName;

		this.anchor = new String[3];
		for ( int d = 0; d < 3; d++ )
		{
			anchor[ d ] = objectName + "_" + ANCHOR[ d ];
		}

		this.labelId = objectName + "_" + LABEL_ID;
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
			final Matcher matcher = LOCATION_X_PATTERN.matcher( column );
			if( matcher.matches() )
				return true;
		}

		return false;
	}

	public static String getObjectName( Collection< String > columns )
	{
		for ( String column : columns )
		{
			final Matcher matcher = LOCATION_X_PATTERN.matcher( column );
			if( matcher.matches() )
				return matcher.group( OBJECT );
		}

		return null;
	}

}
