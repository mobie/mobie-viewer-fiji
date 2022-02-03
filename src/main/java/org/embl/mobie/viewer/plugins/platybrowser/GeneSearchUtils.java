package org.embl.mobie.viewer.plugins.platybrowser;

import de.embl.cba.tables.TableUIs;
import ij.IJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class GeneSearchUtils
{
	public static final String PROSPR = "prospr-";

	private static JTable table;
	private static DefaultTableModel model;

	public static void logGeneExpression(
			double[] micrometerPosition,
			double micrometerRadius,
			Map< String, Double > geneExpressionLevels )
	{
		IJ.log( "\n# Expression levels [fraction of search volume]" );
		IJ.log( "Center position [um]: " + Arrays.toString(  micrometerPosition ) );
		IJ.log( "Radius [um]: " + micrometerRadius );

		final LinkedHashMap< String, Double > sortedExpressionLevels = new LinkedHashMap<>( );

		geneExpressionLevels.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(( Comparator.reverseOrder())))
				.forEachOrdered(x -> sortedExpressionLevels.put(x.getKey(), x.getValue()));

		for ( String gene : sortedExpressionLevels.keySet() )
		{
			if ( sortedExpressionLevels.get( gene ) > 0.0 )
				IJ.log( gene + ": " + String.format( "%.2f", sortedExpressionLevels.get( gene ) ) );
		}
	}

	public static synchronized void addRowToGeneExpressionTable(
			double[] micrometerPosition,
			double micrometerRadius,
			Map< String, Double > expressionLevels )
	{

		final ArrayList< String > sortedGeneNames = sort( expressionLevels.keySet() );

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

	public static ArrayList< String > sort( Collection< String > strings )
	{
		final ArrayList< String > sorted = new ArrayList<>( strings );
		Collections.sort( sorted, String.CASE_INSENSITIVE_ORDER  );
		return sorted;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		final ArrayList< Map.Entry< K, V > > list = new ArrayList<>( map.entrySet() );
		list.sort( Map.Entry.comparingByValue() );

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
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

	public static < T extends RealType< T > & NativeType< T > >
	double getFractionOfNonZeroVoxels( final RandomAccessibleInterval< T > rai, double[] position, double radius, double calibration )
	{
		// TODO: add out-of-bounds strategy or is this handled by the Neighborhood?
		final HyperSphereShape sphereShape = new HyperSphereShape( ( int ) Math.ceil( radius / calibration ) );
		final RandomAccessible< Neighborhood< T > > nra = sphereShape.neighborhoodsRandomAccessible( rai );
		final RandomAccess< Neighborhood< T > > neighborhoodRandomAccess = nra.randomAccess();
		neighborhoodRandomAccess.setPosition( getPixelPosition( position, calibration ) );

		final Neighborhood< T > neighborhood = neighborhoodRandomAccess.get();
		final Cursor< T > cursor = neighborhood.cursor();

		long numberOfNonZeroVoxels = 0;
		long numberOfVoxels = 0;

		while( cursor.hasNext() )
		{
			numberOfVoxels++;

			final double realDouble = cursor.next().getRealDouble();

			if ( realDouble != 0)
			{
				numberOfNonZeroVoxels++;
			}
		}

		return 1.0 * numberOfNonZeroVoxels / numberOfVoxels;
	}

	private static long[] getPixelPosition( double[] position, double calibration )
	{
		long[] pixelPosition = new long[ position.length ];
		for ( int d = 0; d < position.length; ++d )
		{
			pixelPosition[ d ] = (long) ( position[ d ] / calibration );
		}
		return pixelPosition;
	}

	public static String[] combine(String[] a, String[] b){
		int length = a.length + b.length;
		String[] result = new String[length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	public static Object[] combine(Object[] a, Object[] b){
		int length = a.length + b.length;
		Object[] result = new Object[length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	private static String[] extractGeneNamesFromImageSourcesNames( String[] genes )
	{
		for ( int i = 0; i < genes.length; i++ )
			genes[ i ] = getSimplifiedSourceName( genes[i ], true );

		return genes;
	}

	public static String getSimplifiedSourceName( String selectionName, boolean isRemovePROSPR )
	{
		if ( isRemovePROSPR )
			selectionName = selectionName.replace( PROSPR, "" );

		selectionName = selectionName.replace( "6dpf-1-whole-", "" );
		selectionName = selectionName.replace( "segmented-", "" );
		selectionName = selectionName.replace( "mask-", "" );
		return selectionName;
	}

	public static ArrayList< String > extractGeneNamesFromImageSourcesNames( Collection< String > sourceNames )
	{
		final ArrayList< String > geneNames = new ArrayList<>();

		for ( String sourceName : sourceNames )
			geneNames.add( getSimplifiedSourceName( sourceName, true ));

		return geneNames;
	}

}
