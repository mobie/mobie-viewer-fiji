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
package org.embl.mobie.lib.table;

import com.google.gson.annotations.SerializedName;
import ij.IJ;
import org.embl.mobie.lib.table.columns.*;
import org.embl.mobie.lib.table.saw.TableOpener;

import java.util.Collection;

import static org.embl.mobie.lib.table.TableDataFormatNames.RESULTS_TABLE;
import static org.embl.mobie.lib.table.TableDataFormatNames.TABLE;

/**
 * Note that this does not fully specify the table file format.
 *
 * Use the static methods to further specify the format:
 * - {@code getSegmentColumnNames()}
 *
 */
public enum TableDataFormat
{
	@SerializedName( TableDataFormatNames.TSV )
	TSV,
	@SerializedName( TableDataFormatNames.CSV )
	CSV,
	@SerializedName( TableDataFormatNames.PARQUET )
	PARQUET,
	@SerializedName( TableDataFormatNames.EXCEL )
	EXCEL,
	@SerializedName( TableDataFormatNames.RESULTS_TABLE )
	ResultsTable,  // ResultsTable in memory
	@SerializedName( TableDataFormatNames.TABLE )
	Table;  // tablesaw table in memory

	public static final String MOBIE_DEFAULT_CHUNK = "default.tsv";

	@Override
	public String toString()
	{
		switch ( this )
		{
			case ResultsTable:
				return RESULTS_TABLE;
			case Table:
				return TABLE;
			case CSV:
				return TableDataFormatNames.CSV;
			case EXCEL:
				return TableDataFormatNames.EXCEL;
			case TSV:
			default:
				return TableDataFormatNames.TSV;
		}
	}

	public static TableDataFormat fromPath( String path )
	{
		if ( path.toLowerCase().endsWith( ".parquet" ) )
		{
			return TableDataFormat.PARQUET;
		}
		else if ( path.toLowerCase().endsWith( ".xlsx" ) )
		{
			return TableDataFormat.EXCEL;
		}
		else
		{
			Character delimiter = TableOpener.determineDelimiter( path );
			if ( delimiter.equals( ',' ) ) return CSV;
			if ( delimiter.equals( '\t' ) ) return TSV;
		}

		throw new RuntimeException("Could not determine table format of " + path );
	}

	public Character getSeparator()
	{
		switch ( this )
		{
			case CSV:
				return ',';
			case TSV:
			default:
				return '\t';
		}
	}

	public static Character getSeparator( String path )
	{
		if ( path.endsWith( ".csv" ) ) return ',';
		if ( path.endsWith( ".tsv" ) ) return '\t';
		throw new RuntimeException("Could not determine table separator of " + path );
	}

	public static SegmentColumnNames getSegmentColumnNames( Collection< String > columnNames )
	{
		if ( MoBIESegmentColumnNames.matches( columnNames ) )
		{
			return new MoBIESegmentColumnNames();
		}

		if ( MorphoLibJSegmentColumnNames.matches( columnNames ) )
		{
			return new MorphoLibJSegmentColumnNames();
		}

		if ( SkimageSegmentColumnNames.matches( columnNames ) ) 
		{
			return new SkimageSegmentColumnNames( columnNames );
		}

		if ( IlastikTrackingSegmentColumnNames.matches( columnNames ) )
		{
			return new IlastikTrackingSegmentColumnNames();
		}

		if ( IlastikSegmentColumnNames.matches( columnNames ) )
		{
			return new IlastikSegmentColumnNames();
		}

		if ( CellProfilerSegmentColumnNames.matches( columnNames ) )
		{
			return new CellProfilerSegmentColumnNames( columnNames );
		}

		if ( MicrogliaSegmentColumnNames.matches( columnNames ) )
		{
			return new MicrogliaSegmentColumnNames();
		}

		return null;
	}
}
