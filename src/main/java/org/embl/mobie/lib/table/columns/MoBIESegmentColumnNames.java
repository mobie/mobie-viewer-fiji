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

import org.embl.mobie.lib.table.ColumnNames;

import java.util.Collection;

public class MoBIESegmentColumnNames implements SegmentColumnNames
{
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
