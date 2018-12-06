package de.embl.cba.platynereis;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.platynereis.ui.BdvTextOverlay;
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
	private final Map< String, PlatynereisDataSource > dataSources;
	private final Bdv bdv;
	private final int mipMapLevel;
	private final double micrometerVoxelSize;
	private BdvTextOverlay bdvTextOverlay;
	private boolean searchFinished;
	private ArrayList< String > sortedNames;
	private Map< String, Double > sortedGenes;


	public GeneSearch( double micrometerRadius,
					   double[] micrometerPosition,
					   Map< String, PlatynereisDataSource > dataSources,
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
		this.searchFinished = false;
	}

	public void run( )
	{

		// TODO: throws exeception when removing
		bdvTextOverlay = new BdvTextOverlay( bdv, "Searching expressed genes; please wait...", micrometerPosition );

		(new Thread(new Runnable(){
			public void run(){
				runSearch();
			}
		})).start();

	}

	public Map< String, Double > getSortedGenes()
	{
		return sortedGenes;
	}

	public boolean isDone()
	{
		return searchFinished;
	}

	private void runSearch( )
	{

		final Set< String > sources = dataSources.keySet();

		Map< String, Double > localSums = new LinkedHashMap<>(  );

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

			final PlatynereisDataSource source = dataSources.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );
			final RandomAccessibleInterval< T > image = (RandomAccessibleInterval<T>) setupImgLoader.getImage( 0, mipMapLevel );

			final double localMaximum = Utils.getLocalSum(
					image,
					micrometerPosition,
					micrometerRadius,
					micrometerVoxelSize );

			localSums.put( name, localMaximum );

		}

		sortedGenes = Utils.sortByValue( localSums );
		sortedNames = new ArrayList( sortedGenes.keySet() );

		removeGenesWithZeroExpression();
		logResult( );

		searchFinished = true;

		bdvTextOverlay.setText( "" );
		//bdvTextOverlay.removeFromBdv(); // TODO: throws error
	}

	private void logResult()
	{
		Utils.log( "## Sorted gene list " );
		sortedNames = new ArrayList( sortedGenes.keySet() );
		for ( int i = 0; i < sortedGenes.size(); ++i )
		{
			String name = sortedNames.get( i );
			Utils.log( name + ": " + sortedGenes.get( name ) );
		}
	}

	private void removeGenesWithZeroExpression()
	{
		// remove entries with zero expression
		for ( int i = sortedNames.size() - 1; i >= 0; --i )
		{
			String name = sortedNames.get( i );
			double value = sortedGenes.get( name ).doubleValue();

			if ( value == 0.0 )
			{
				sortedGenes.remove( name );
			}
		}
	}


}
