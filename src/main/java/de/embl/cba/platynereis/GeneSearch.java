package de.embl.cba.platynereis;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.util.Bdv;
import de.embl.cba.platynereis.ui.BdvTextOverlay;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.platynereis.Utils.openSpimData;

public class GeneSearch < T extends RealType< T > & NativeType< T > >
{

	private final double micrometerRadius;
	private final double[] micrometerPosition;
	private final Map< String, PlatynereisDataSource > dataSources;
	private final Bdv bdv;
	private final int mipMapLevel;
	private final double micrometerVoxelSize;


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
	}

	public void run( )
	{

		BdvTextOverlay bdvTextOverlay = new BdvTextOverlay( this.bdv, "Searching genes...", micrometerPosition );

		IJ.wait( 2000 );

		bdvTextOverlay.removeFromBdv();
	}

	private void search( )
	{

		final Set< String > sources = dataSources.keySet();
		Map< String, Double > localMaxima = new LinkedHashMap<>(  );

		for ( String name : sources )
		{

			if ( name.contains( Constants.EM_FILE_ID ) ) continue;

			final PlatynereisDataSource source = dataSources.get( name );

			if ( source.spimData == null )
			{
				source.spimData = openSpimData( source.file );
			}

			final ViewerImgLoader imgLoader = ( ViewerImgLoader ) source.spimData.getSequenceDescription().getImgLoader();
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( 0 );
			final RandomAccessibleInterval< T > image = (RandomAccessibleInterval<T>) setupImgLoader.getImage( 0, mipMapLevel );

			final double localMaximum = Utils.getLocalMaximum(
					image,
					micrometerPosition,
					micrometerRadius,
					micrometerVoxelSize,
					name );

			localMaxima.put( name, localMaximum );

		}

		final Map< String, Double > sortedMaxima = Utils.sortByValue( localMaxima );
		final ArrayList sortedNames = new ArrayList( sortedMaxima.keySet() );

		Utils.log( "## Nearby gene list " );
		for ( int i = 0; i < sortedMaxima.size(); ++i )
		{
			String name = ( String ) sortedNames.get( i );
			Utils.log( name + ": " + sortedMaxima.get( name ) );
		}

//		mainCommand.addSourceToBdv(  );
	}

}
