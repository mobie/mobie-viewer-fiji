package de.embl.cba.platynereis.ui;

import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.objects.ObjectTablePanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Map;

import static de.embl.cba.platynereis.utils.Utils.combine;

public class GeneExpressions
{
	// TODO make a real class

	public static ObjectTablePanel geneExpressionTablePanel;

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
		if ( geneExpressionTablePanel == null )
		{
			initGeneExpressionTable( geneExpressionLevels );
		}

		final Double[] position = { micrometerPosition [ 0 ], micrometerPosition[ 1 ], micrometerPosition[ 2 ], 0.0 };
		final Double[] parameters = { micrometerRadius };
		final Double[] expressionLevels = geneExpressionLevels.values().toArray( new Double[ geneExpressionLevels.size() ] );
		((DefaultTableModel)geneExpressionTablePanel.getTable().getModel()).addRow( combine( combine( position, parameters ), expressionLevels ) );
	}

	public static void initGeneExpressionTable( Map< String, Double > geneExpressionLevels )
	{
		final String[] position = { "X", "Y", "Z", "T" };
		final String[] searchParameters = { "SearchRadius_um" };
		final String[] genes = geneExpressionLevels.keySet().toArray( new String[ geneExpressionLevels.keySet().size() ] );

		final DefaultTableModel model = new DefaultTableModel();
		model.setColumnIdentifiers(  combine( combine( position, searchParameters ), genes )  );
		final JTable table = new JTable( model );
		geneExpressionTablePanel = new ObjectTablePanel( table );
	}
}
