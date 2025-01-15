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
package org.embl.mobie.command;

import ij.IJ;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

import java.awt.*;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ResultsTableFetcher
{
	public ResultsTable fetch( String tableTitle )
	{
		final HashMap< String, ResultsTable > titleToTable = fetchCurrentlyOpenResultsTables();

		if ( ! titleToTable.containsKey( tableTitle ) )
		{
			throwResultsTableNotErrorMessage( titleToTable, tableTitle );
			throw new UnsupportedOperationException("Results table not found: " + tableTitle );
		}
		else
		{
			return titleToTable.get( tableTitle );
		}
	}

	public static void throwResultsTableNotErrorMessage( HashMap< String, ResultsTable > titleToTable, String parentTable )
	{
		IJ.error( "The results table " + parentTable + "  does not exist.\n" +
				"Please choose one of the following:\n" + titleToTable.keySet().stream().collect( Collectors.joining("\n") ) );
	}

	public HashMap< String, ij.measure.ResultsTable > fetchCurrentlyOpenResultsTables()
	{
		HashMap< String, ij.measure.ResultsTable > titleToResultsTable = new HashMap<>();

		final Frame[] nonImageWindows = WindowManager.getNonImageWindows();
		for ( Frame nonImageWindow : nonImageWindows )
		{
			if ( nonImageWindow instanceof TextWindow )
			{
				final TextWindow textWindow = ( TextWindow ) nonImageWindow;

				final ij.measure.ResultsTable resultsTable = textWindow.getResultsTable();

				if ( resultsTable != null )
					titleToResultsTable.put( resultsTable.getTitle(), resultsTable );
			}
		}

		return titleToResultsTable;
	}
}
