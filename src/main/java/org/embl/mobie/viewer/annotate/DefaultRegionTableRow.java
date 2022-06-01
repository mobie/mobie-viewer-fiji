/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.annotate;

import de.embl.cba.tables.tablerow.AbstractTableRow;
import net.imglib2.roi.RealMaskRealInterval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultRegionTableRow extends AbstractTableRow implements RegionTableRow
{
	protected final RealMaskRealInterval mask;
	protected final Map< String, String > cells;
	protected final String name;

	// TODO: rename to annotated region?
	public DefaultRegionTableRow(
			String name,
			RealMaskRealInterval mask,
			Map< String, List< String > > columns,
			int rowIndex )
	{
		this.name = name;
		this.mask = mask;

		// set cells
		this.cells = new LinkedHashMap<>();
		for ( String column : columns.keySet() )
			cells.put( column, columns.get( column ).get( rowIndex ) );

	}

	@Override
	public RealMaskRealInterval mask()
	{
		return mask;
	}

	@Override
	public Integer timePoint()
	{
		if ( cells.containsKey( "timepoint" ) )
			return Integer.parseInt( cells.get( "timepoint" ) );
		else
			return 0;
	}

	@Override
	public String name()
	{
		return name;
	}

	@Override
	public String getCell( String columnName )
	{
		return cells.get( columnName );
	}

	@Override
	public void setCell( String columnName, String value )
	{
		cells.put( columnName, value );
		notifyCellChangedListeners( columnName, value );
	}

	@Override
	public Set< String > getColumnNames()
	{
		return cells.keySet();
	}

	@Override
	@Deprecated
	public int rowIndex()
	{
		return -1;
	}

}
