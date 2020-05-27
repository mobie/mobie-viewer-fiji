package de.embl.cba.mobie.platybrowser;

import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.TableUIs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

import static de.embl.cba.mobie.utils.Utils.combine;

public class GeneSearchResults
{
	private static JTable table;
	private static DefaultTableModel model;

	public static void logGeneExpression(
			double[] micrometerPosition,
			double micrometerRadius,
			Map< String, Double > geneExpressionLevels )
	{

		Utils.log( "\n# Expression levels [fraction of search volume]" );
		Utils.logVector( "Center position [um]" , micrometerPosition, 2 );
		Utils.log( "Radius [um]: " + micrometerRadius );

		final LinkedHashMap< String, Double > sortedExpressionLevels = new LinkedHashMap<>( );

		geneExpressionLevels.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue((Comparator.reverseOrder())))
				.forEachOrdered(x -> sortedExpressionLevels.put(x.getKey(), x.getValue()));

		for ( String gene : sortedExpressionLevels.keySet() )
		{
			if ( sortedExpressionLevels.get( gene ) > 0.0 )
				Utils.log( gene + ": " + String.format( "%.2f", sortedExpressionLevels.get( gene ) ) );
		}
	}

	public static synchronized void addRowToGeneExpressionTable(
			double[] micrometerPosition,
			double micrometerRadius,
			Map< String, Double > expressionLevels )
	{

		final ArrayList< String > sortedGeneNames = Utils.getSortedList( expressionLevels.keySet() );

		if ( table == null )
		{
			initGeneExpressionTable( sortedGeneNames );
			showGeneExpressionTable();
		}

		final Double[] position = { micrometerPosition [ 0 ], micrometerPosition[ 1 ], micrometerPosition[ 2 ], 0.0 };
		final Double[] parameters = { micrometerRadius };

		final ArrayList< Double > sortedExpressionLevels = new ArrayList<>();
		for ( String geneName : sortedGeneNames )
			sortedExpressionLevels.add( expressionLevels.get( geneName ) );

		model.addRow( combine( combine( position, parameters ), sortedExpressionLevels.toArray( new Double[ expressionLevels.size() ] )));
	}

	public static void showGeneExpressionTable()
	{
		JFrame frame = new JFrame("Gene Expression");

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		final JMenuBar jMenuBar = new JMenuBar();
		final JMenu menu = new JMenu( "File" );
		final JMenuItem saveAs = new JMenuItem( "Save as..." );
		saveAs.addActionListener( e -> SwingUtilities.invokeLater( () -> TableUIs.saveTableUI( table ) ) );
		menu.add( saveAs );
		jMenuBar.add( menu );
		frame.setJMenuBar( jMenuBar );

		JScrollPane tableContainer = new JScrollPane(
				table,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		panel.add(tableContainer, BorderLayout.CENTER);
		frame.getContentPane().add(panel);

		frame.pack();
		frame.setVisible(true);
	}

	public static void initGeneExpressionTable( Collection< String > geneNames )
	{
		final String[] position = { "X", "Y", "Z", "T" };
		final String[] searchParameters = { "SearchRadius_um" };

		String[] genes = geneNames.toArray( new String[ geneNames.size() ] );

		model = new DefaultTableModel();
		model.setColumnIdentifiers( combine( combine( position, searchParameters ), genes )  );
		table = new JTable( model );
	}

	public static String[] extractGeneNamesFromImageSourcesNames( String[] genes )
	{
		for ( int i = 0; i < genes.length; i++ )
			genes[ i ] = Utils.getSimplifiedSourceName( genes[i ], true );

		return genes;
	}

	public static ArrayList< String > extractGeneNamesFromImageSourcesNames( Collection< String > sourceNames )
	{
		final ArrayList< String > geneNames = new ArrayList<>();

		for ( String sourceName : sourceNames )
			geneNames.add( Utils.getSimplifiedSourceName( sourceName, true ));

		return geneNames;
	}

}
