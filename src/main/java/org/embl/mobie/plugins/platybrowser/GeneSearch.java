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
package org.embl.mobie.plugins.platybrowser;

import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.util.ThreadHelper;
import org.embl.mobie.lib.serialize.DataSource;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.View;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.ui.UserInterfaceHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static org.embl.mobie.plugins.platybrowser.GeneSearch.GeneSearchUtils.getFractionOfNonZeroVoxels;
import static org.embl.mobie.plugins.platybrowser.GeneSearch.GeneSearchUtils.sortByValue;

public class GeneSearch
{
	private static final String PROSPR_UI_SELECTION_GROUP = "prospr";

	private final double micrometerRadius;
	private final double[] micrometerPosition;
	private final MoBIE moBIE;
	private Map< String, Double > localExpression;
	private Set< String > prosprSourceNames;
	private static HashMap< String, Image< ? > > prosprSources;

	public GeneSearch( double micrometerRadius,
					   double[] micrometerPosition,
					   MoBIE moBIE )
	{
		this.micrometerRadius = micrometerRadius;
		this.micrometerPosition = micrometerPosition;
		this.moBIE = moBIE;
	}

	public void searchGenes( )
	{
		prosprSourceNames = fetchProsprSourceNames();

		// TODO: One should not open the raw sources but the views,
		//  because there may be additional transformations in the views.
		//  This could be done using those methods:
		//  moBIE.getViewManager().openAndTransformViewSources( view );
		//  Since the Prospr sources are not transformed, this does not matter (yet)...
		if ( prosprSources == null )
		{
			prosprSources = new HashMap<>();
			moBIE.initDataSources( moBIE.getDataSources( prosprSourceNames ) );
			for ( String prosprSourceName : prosprSourceNames )
				prosprSources.put( prosprSourceName, DataStore.getImage( prosprSourceName ) );
		}

		final Map< String, Double > geneExpressionLevels = runSearchAndGetLocalExpression( prosprSources );

		GeneSearchUtils.addRowToGeneExpressionTable( micrometerPosition, micrometerRadius, geneExpressionLevels );

		GeneSearchUtils.logGeneExpression( micrometerPosition, micrometerRadius, geneExpressionLevels );
	}

	private Set< String> fetchProsprSourceNames()
	{
		final Map< String, Map< String, View > > groupingsToViews = moBIE.getUserInterface().getGroupingsToViews();
		return groupingsToViews.get( PROSPR_UI_SELECTION_GROUP ).keySet();
	}

	private Map< String, Double > getExpressionLevelsSortedByValue()
	{
		Map< String, Double > localSortedExpression = sortByValue( localExpression );
		removeGenesWithZeroExpression( localSortedExpression );
		return localSortedExpression;
	}

	private Map< String, Double > runSearchAndGetLocalExpression( HashMap< String, Image< ? > > images )
	{
		localExpression = new ConcurrentHashMap<>();

		IJ.log( "# Gene search" );
		final ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
		for ( String gene : images.keySet() )
		{
			futures.add(
				ThreadHelper.executorService.submit( () -> {
					searchGene( images.get( gene ) );
			}));
		}
		ThreadHelper.waitUntilFinished( futures );

		return localExpression;
	}

	private void searchGene( Image< ? > image )
	{
		final bdv.viewer.Source source = image.getSourcePair().getSource();

		final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );

		final VoxelDimensions voxelDimensions = source.getVoxelDimensions();

		final double fractionOfNonZeroVoxels = getFractionOfNonZeroVoxels(
				( RandomAccessibleInterval ) rai,
				micrometerPosition,
				micrometerRadius,
				voxelDimensions.dimension( 0 ) );

		localExpression.put( source.getName(), fractionOfNonZeroVoxels );

		IJ.log( "Gene Search: Fraction of non-zero voxels in search region: " + source.getName() + ": " + fractionOfNonZeroVoxels );
	}

	private void removeGenesWithZeroExpression( Map< String, Double > localSortedExpression)
	{
		ArrayList< String > sortedNames = new ArrayList( localSortedExpression.keySet() );

		// remove entries with zero expression
		for ( int i = sortedNames.size() - 1; i >= 0; --i )
		{
			String name = sortedNames.get( i );
			double value = localSortedExpression.get( name ).doubleValue();

			if ( value == 0.0 )
			{
				localSortedExpression.remove( name );
			}
		}
	}

	// TODO: It makes no sense to have this as an extra class => move all methods into GeneSearch
	public static class GeneSearchUtils
	{
		private static Map< String, SourceAndConverter< ? > > prosprSources;

		private static JTable table;
		private static DefaultTableModel model;
		private static ArrayList< String > prosprSourceNames;
		private static MoBIE moBIE;

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
			saveAs.addActionListener( e -> SwingUtilities.invokeLater( () -> UserInterfaceHelper.saveTableUI( table ) ) );
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

		public static void setProsprSourceNames( ImageDataFormat imageDataFormat, Dataset dataset )
		{
			GeneSearchUtils.prosprSourceNames = new ArrayList<>();
			for ( String sourceName : dataset.sources().keySet() )
			{
				final DataSource dataSource = dataset.sources().get( sourceName );
				if ( ! ( dataSource instanceof ImageDataSource ) ) continue;
				final ImageDataSource imageSource = ( ImageDataSource ) dataSource;
				final String relativePath = imageSource.imageData.get( imageDataFormat ).relativePath;

				if ( relativePath.contains( PROSPR_UI_SELECTION_GROUP ) )
				{
					prosprSourceNames.add( sourceName );
				};
			}
		}

		public static void setMoBIE( MoBIE moBIE )
		{
			GeneSearchUtils.moBIE = moBIE;
		}
	}
}
