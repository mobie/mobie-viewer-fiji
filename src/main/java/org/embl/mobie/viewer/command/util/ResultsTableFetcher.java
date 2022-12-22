package org.embl.mobie.viewer.command.util;

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
		final HashMap< String, ResultsTable > titleToTable = new de.embl.cba.tables.results.ResultsTableFetcher().fetchCurrentlyOpenResultsTables();

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