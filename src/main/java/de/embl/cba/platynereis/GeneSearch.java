package de.embl.cba.platynereis;

import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GeneSearch < T extends RealType< T > & NativeType< T > >
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

	public Map< String, Double > getExpressionLevelsSortedByValue()
	{
		Map< String, Double > localSortedExpression = Utils.sortByValue( localExpression );
		removeGenesWithZeroExpression( localSortedExpression );
		return localSortedExpression;
	}

	public Map< String, Double > runSearchAndGetLocalExpression()
	{
		final Set< String > sourceNames = imageSourcesModel.sources().keySet();

		localExpression = new LinkedHashMap<>(  );

		Logger.info( "# Gene search" );
		for ( String sourceName : sourceNames )
		{

			// TODO: How to better define which images are gene searched?
			if ( sourceName.contains( Constants.SEGMENTED ) ) continue;
			if ( ! sourceName.contains( Constants.PROSPR ) ) continue;

			final SourceAndMetadata sourceAndMetadata =
					imageSourcesModel.sources().get( sourceName );

			final RandomAccessibleInterval< ? > rai =
					BdvUtils.getRealTypeNonVolatileRandomAccessibleInterval(
							sourceAndMetadata.source(), 0, 0 );

			final VoxelDimensions voxelDimensions = sourceAndMetadata.source().getVoxelDimensions();

			final double fractionOfNonZeroVoxels = Utils.getFractionOfNonZeroVoxels(
					( RandomAccessibleInterval ) rai,
					micrometerPosition,
					micrometerRadius,
					voxelDimensions.dimension( 0 ) );

			final String simplifiedSourceName = Utils.getSimplifiedSourceName( sourceName, true );
			localExpression.put( simplifiedSourceName, fractionOfNonZeroVoxels );
			Logger.info(simplifiedSourceName + ": " + fractionOfNonZeroVoxels );
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
