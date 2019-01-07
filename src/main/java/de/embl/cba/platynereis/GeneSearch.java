package de.embl.cba.platynereis;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.platynereis.utils.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.*;

import static de.embl.cba.platynereis.utils.Utils.openSpimData;

public class GeneSearch < T extends RealType< T > & NativeType< T > >
{

	private final double micrometerRadius;
	private final double[] micrometerPosition;
	private final Map< String, PlatySource > dataSources;
	private final Bdv bdv;
	private final int mipMapLevel;
	private final double micrometerVoxelSize;
	private Map< String, Double > localExpression;

	public GeneSearch( double micrometerRadius,
					   double[] micrometerPosition,
					   Map< String, PlatySource > dataSources,
					   Bdv bdv,
					   int mipMapLevel,
					   double micrometerVoxelSize )
	{
		this.micrometerRadius = micrometerRadius;
		this.micrometerPosition = micrometerPosition;
		this.dataSources = dataSources;
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
		final Set< String > sources = dataSources.keySet();

		localExpression = new LinkedHashMap<>(  );

		for ( String name : sources )
		{
			if ( name.contains( Constants.EM_FILE_ID ) ) continue;
			if ( ! name.contains( Constants.MEDS ) ) continue;
			if ( name.contains( Constants.OLD ) ) continue;

			(new Thread(new Runnable(){
				public void run(){
					Utils.log( "Examining " + name );
				}
			})).start();

			final PlatySource source = dataSources.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );
			final RandomAccessibleInterval< T > image = (RandomAccessibleInterval<T>) setupImgLoader.getImage( 0, mipMapLevel );

			final double fractionOfNonZeroVoxels = Utils.getFractionOfNonZeroVoxels(
					image,
					micrometerPosition,
					micrometerRadius,
					micrometerVoxelSize );

			localExpression.put( name, fractionOfNonZeroVoxels );

		}

		return localExpression;

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


}
