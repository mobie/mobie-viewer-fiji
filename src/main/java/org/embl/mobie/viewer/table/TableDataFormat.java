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
package org.embl.mobie.viewer.table;

import com.google.gson.annotations.SerializedName;
import org.embl.mobie.viewer.table.columns.MoBIESegmentColumnNames;
import org.embl.mobie.viewer.table.columns.MorpholibJSegmentColumnNames;
import org.embl.mobie.viewer.table.columns.SegmentColumnNames;

import static org.embl.mobie.viewer.table.TableDataFormatNames.*;

public enum TableDataFormat
{
	@SerializedName( TSV )
	MoBIETSV,  // TSV file with MoBIE column names
	@SerializedName( MORPHOLIBJCSV )
	MorpholibJCSV, // CSV file with MorpholibJ column names
	@SerializedName( MORPHOLIBJ )
	MorpholibJ; // RAM resident table with MorpholibJ column names

	public static final String DEFAULT_TSV = "default.tsv";

	@Override
	public String toString()
	{
		switch ( this )
		{
			case MorpholibJ:
				return MORPHOLIBJ;
			case MorpholibJCSV:
				return MORPHOLIBJCSV;
			case MoBIETSV:
			default:
				return TSV;
		}
	}

	public static TableDataFormat fromString( String name )
	{
		switch ( name )
		{
			case MORPHOLIBJ:
				return MorpholibJ;
			case MORPHOLIBJCSV:
				return TableDataFormat.MorpholibJCSV;
			case TSV:
			default:
				return TableDataFormat.MoBIETSV;
		}
	}


	public Character getSeparator()
	{
		switch ( this )
		{
			case MoBIETSV:
			default:
				return '\t';
		}
	}

	public SegmentColumnNames getSegmentColumnNames()
	{
		switch ( this )
		{
			case MorpholibJ:
			case MorpholibJCSV:
				return new MorpholibJSegmentColumnNames();
			case MoBIETSV:
			default:
				return new MoBIESegmentColumnNames();
		}
	}
}
