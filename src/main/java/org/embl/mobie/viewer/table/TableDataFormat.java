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
import org.apache.commons.lang3.StringUtils;
import org.embl.mobie.viewer.table.columns.MoBIESegmentColumnNames;
import org.embl.mobie.viewer.table.columns.MorpholibJSegmentColumnNames;
import org.embl.mobie.viewer.table.columns.SegmentColumnNames;

import java.util.Collection;

import static org.embl.mobie.viewer.table.TableDataFormatNames.*;

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
	TSV,  // TSV file
	@SerializedName( TableDataFormatNames.CSV )
	CSV,  // CSV file
	@SerializedName( TableDataFormatNames.RESULTS_TABLE )
	ResultsTable;  // ResultsTable in memory

//	// TODO: Remove the below?
//	@SerializedName( SKIMAGE_TSV )
//	SkimageTSV, // Skimage region props CSV FILE
//	@SerializedName( MLJ_CSV )
//	MorphoLibJCSV, // MorpholibJ CSV FILE
//	@SerializedName( MLJ_RESULTS_TABLE )
//	MorphoLibJResultsTable; // MorpholibJ ResultsTable in RAM

	public static final String DEFAULT_TSV = "default.tsv";

	@Override
	public String toString()
	{
		switch ( this )
		{
			case ResultsTable:
				return RESULTS_TABLE;
			case CSV:
				return TableDataFormatNames.CSV;
			case TSV:
			default:
				return TableDataFormatNames.TSV;
		}
	}

	public static TableDataFormat fromString( String string )
	{
		switch ( string )
		{
			case RESULTS_TABLE:
				return ResultsTable;
			case TableDataFormatNames.CSV:
				return CSV;
			case TableDataFormatNames.TSV:
			default:
				return TableDataFormat.TSV;
		}
	}

	public static TableDataFormat fromPath( String path )
	{
		if ( path.endsWith( ".csv" ) ) return CSV;
		if ( path.endsWith( ".tsv" ) ) return TSV;
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
			return new MoBIESegmentColumnNames();

		if ( MorpholibJSegmentColumnNames.matches( columnNames ) )
			return new MoBIESegmentColumnNames();

		throw new UnsupportedOperationException( "Could not match column names. " + StringUtils.join( columnNames ) );
	}
}
