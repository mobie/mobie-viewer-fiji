package de.embl.cba.platynereis;

import bdv.VolatileSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.modelview.images.ImageSourcesModel;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GeneSearch < T extends RealType< T > & NativeType< T > >
{

	private final double micrometerRadius;
	private final double[] micrometerPosition;
	private final ImageSourcesModel imageSourcesModel;
	private final double micrometerVoxelSize;
	private Map< String, Double > localExpression;

	public GeneSearch( double micrometerRadius,
					   double[] micrometerPosition,
					   ImageSourcesModel imageSourcesModel,
					   double micrometerVoxelSize )
	{
		this.micrometerRadius = micrometerRadius;
		this.micrometerPosition = micrometerPosition;
		this.imageSourcesModel = imageSourcesModel;
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
			if ( ! sourceName.contains( Constants.MED ) ) continue;

			logProgress( sourceName );

			final SourceAndMetadata sourceAndMetadata =
					imageSourcesModel.sources().get( sourceName );

			final RandomAccessibleInterval< ? > rai = getRandomAccessibleInterval( sourceAndMetadata );

			final double fractionOfNonZeroVoxels = Utils.getFractionOfNonZeroVoxels(
					( RandomAccessibleInterval ) rai,
					micrometerPosition,
					micrometerRadius,
					micrometerVoxelSize );

			localExpression.put( sourceName, fractionOfNonZeroVoxels );
		}

		return localExpression;

	}

	public RandomAccessibleInterval< ? > getRandomAccessibleInterval( SourceAndMetadata sourceAndMetadata )
	{
		final PlatynereisImageSourcesModel.LazySpimSource lazySpimSource =
				( PlatynereisImageSourcesModel.LazySpimSource ) sourceAndMetadata.source();
		final TransformedSource transformedSource = (TransformedSource) lazySpimSource.wrappedSource();
		final VolatileSpimSource wrappedSource = ( VolatileSpimSource ) transformedSource.getWrappedSource();
		final RandomAccessibleInterval< ? > rai = wrappedSource.nonVolatile().getSource( 0, 0 );

		return rai;
	}

	public void logProgress( String sourceName )
	{
		// SwingUtilities.invokeLater( () -> Utils.log( "Examining " + sourceName ) );
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
