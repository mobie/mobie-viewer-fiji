package de.embl.cba.platynereis;

import de.embl.cba.platynereis.utils.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Map;

import static de.embl.cba.platynereis.utils.Utils.combine;

public class GeneSearchResults
{
	private static JTable table;
	private static DefaultTableModel model;

	public static void logGeneExpression( double[] micrometerPosition, double micrometerRadius, Map< String, Double > sortedGeneExpressionLevels )
	{
		Utils.log( "\n# Expression levels [fraction of search volume]" );
		Utils.logVector( "Center position [um]" , micrometerPosition );
		Utils.log( "Radius [um]: " + micrometerRadius );
		for ( String gene : sortedGeneExpressionLevels.keySet() )
		{
			Utils.log( gene  + ": " + sortedGeneExpressionLevels.get( gene ) );
		}
	}

	public static synchronized void addRowToGeneExpressionTable( double[] micrometerPosition, double micrometerRadius, Map< String, Double > geneExpressionLevels )
	{
		if ( table == null )
		{
			initGeneExpressionTable( geneExpressionLevels );
		}

		final Double[] position = { micrometerPosition [ 0 ], micrometerPosition[ 1 ], micrometerPosition[ 2 ], 0.0 };
		final Double[] parameters = { micrometerRadius };
		final Double[] expressionLevels = geneExpressionLevels.values().toArray( new Double[ geneExpressionLevels.size() ] );
		model.addRow( combine( combine( position, parameters ), expressionLevels ) );
	}

	public static void initGeneExpressionTable( Map< String, Double > geneExpressionLevels )
	{
		final String[] position = { "X", "Y", "Z", "T" };
		final String[] searchParameters = { "SearchRadius_um" };
		final String[] genes = geneExpressionLevels.keySet().toArray( new String[ geneExpressionLevels.keySet().size() ] );

		model = new DefaultTableModel();
		model.setColumnIdentifiers( combine( combine( position, searchParameters ), genes )  );
		table = new JTable( model );
	}

}
