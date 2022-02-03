package org.embl.mobie.viewer.plugins.platybrowser;

import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.IJ;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.embl.mobie.viewer.plugins.platybrowser.GeneSearchUtils.getFractionOfNonZeroVoxels;
import static org.embl.mobie.viewer.plugins.platybrowser.GeneSearchUtils.getSimplifiedSourceName;
import static org.embl.mobie.viewer.plugins.platybrowser.GeneSearchUtils.sortByValue;

public class GeneSearch
{
	private final double micrometerRadius;
	private final double[] micrometerPosition;
	private final ImageSourcesModel imageSourcesModel;
	private Map< String, Double > localExpression;

	public GeneSearch( double micrometerRadius,
					   double[] micrometerPosition,
					   ImageSourcesModel imageSourcesModel )
	{
		this.micrometerRadius = micrometerRadius;
		this.micrometerPosition = micrometerPosition;
		this.imageSourcesModel = imageSourcesModel;
	}

	public void searchGenes( )
	{
		final Map< String, Double > geneExpressionLevels = runSearchAndGetLocalExpression();

		GeneSearchUtils.addRowToGeneExpressionTable(
				micrometerPosition, micrometerRadius, geneExpressionLevels );

		GeneSearchUtils.logGeneExpression(
				micrometerPosition, micrometerRadius, geneExpressionLevels );
	}

	private Map< String, Double > getExpressionLevelsSortedByValue()
	{
		Map< String, Double > localSortedExpression = sortByValue( localExpression );
		removeGenesWithZeroExpression( localSortedExpression );
		return localSortedExpression;
	}

	private Map< String, Double > runSearchAndGetLocalExpression()
	{
		final Map< String, SourceAndMetadata< ? > > sources = imageSourcesModel.sources();

		localExpression = new LinkedHashMap<>(  );

		IJ.log( "# Gene search" );
		for ( String sourceName : sources.keySet() )
		{
			final Metadata.Type type = sources.get( sourceName ).metadata().type;
			if ( ! type.equals( Metadata.Type.Image )  ) continue;
			if ( ! sourceName.contains( GeneSearchUtils.PROSPR ) ) continue;

			final SourceAndMetadata sourceAndMetadata =
					sources.get( sourceName );

			final RandomAccessibleInterval< ? > rai =
					BdvUtils.getRealTypeNonVolatileRandomAccessibleInterval(
							sourceAndMetadata.source(), 0, 0 );

			final VoxelDimensions voxelDimensions = sourceAndMetadata.source().getVoxelDimensions();

			final double fractionOfNonZeroVoxels = getFractionOfNonZeroVoxels(
					( RandomAccessibleInterval ) rai,
					micrometerPosition,
					micrometerRadius,
					voxelDimensions.dimension( 0 ) );

			final String simplifiedSourceName = getSimplifiedSourceName( sourceName, true );
			localExpression.put( simplifiedSourceName, fractionOfNonZeroVoxels );
			IJ.log(simplifiedSourceName + ": " + fractionOfNonZeroVoxels );
		}

		return localExpression;
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
}
