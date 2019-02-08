package de.embl.cba.platynereis;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import bdv.viewer.Source;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.modelview.images.ImageSourcesModel;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import de.embl.cba.tables.objects.ObjectTablePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.*;

import static de.embl.cba.platynereis.utils.Utils.combine;

public class GeneSearch < T extends RealType< T > & NativeType< T > >
{

	private final double micrometerRadius;
	private final double[] micrometerPosition;
	private final ImageSourcesModel imageSourcesModel;
	private final Bdv bdv;
	private final int mipMapLevel;
	private final double micrometerVoxelSize;
	private Map< String, Double > localExpression;

	public GeneSearch( double micrometerRadius,
					   double[] micrometerPosition,
					   ImageSourcesModel imageSourcesModel,
					   Bdv bdv,
					   int mipMapLevel,
					   double micrometerVoxelSize )
	{
		this.micrometerRadius = micrometerRadius;
		this.micrometerPosition = micrometerPosition;
		this.imageSourcesModel = imageSourcesModel;
		this.bdv = bdv;
		this.mipMapLevel = mipMapLevel;
		this.micrometerVoxelSize = micrometerVoxelSize;
	}


	public Map< String, Double > getSortedExpressionLevels()
	{
		Map< String, Double > localSortedExpression = Utils.sortByValue( localExpression );
		removeGenesWithZeroExpression( localSortedExpression );
		return localSortedExpression;
	}

	public Map< String, Double > runSearchAndGetLocalExpression()
	{
		final Set< String > sourceNames = imageSourcesModel.sources().keySet();

		localExpression = new LinkedHashMap<>(  );

		for ( String sourceName : sourceNames )
		{
			if ( sourceName.contains( Constants.EM_FILE_ID ) ) continue;
			if ( ! sourceName.contains( Constants.MEDS ) ) continue;
			if ( sourceName.contains( Constants.OLD ) ) continue;

			logProgress( sourceName );

			final SourceAndMetadata sourceAndMetadata = imageSourcesModel.sources().get( sourceName );
			final Source< ? > source = sourceAndMetadata.source();
			final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );

			final double fractionOfNonZeroVoxels = Utils.getFractionOfNonZeroVoxels(
					rai,
					micrometerPosition,
					micrometerRadius,
					micrometerVoxelSize );

			localExpression.put( sourceName, fractionOfNonZeroVoxels );
		}

		return localExpression;

	}

	public void logProgress( String sourceName )
	{
		(new Thread(new Runnable(){
			public void run(){
				Utils.log( "Examining " + sourceName );
			}
		})).start();
	}


//	private void logResult()
//	{
//		Utils.log( "## Sorted gene list " );
//		sortedNames = new ArrayList( localSortedExpression.keySet() );
//		for ( int i = 0; i < localSortedExpression.size(); ++i )
//		{
//			String name = sortedNames.get( i );
//			Utils.log( name + ": " + localSortedExpression.get( name ) );
//		}
//	}

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
		(( DefaultTableModel )geneExpressionTablePanel.getTable().getModel()).addRow( combine( combine( position, parameters ), expressionLevels ) );
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
