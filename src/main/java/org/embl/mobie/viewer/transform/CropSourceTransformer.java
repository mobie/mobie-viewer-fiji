package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;

import java.util.List;
import java.util.Map;

public class CropSourceTransformer extends AbstractSourceTransformer
{
	protected double[] min;
	protected double[] max;
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;
	protected boolean centerAtOrigin = true;

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		for ( String sourceName : sourceNameToSourceAndConverter.keySet() )
		{
			if ( sources.contains( sourceName ) )
			{
				final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );
				String transformedSourceName = getTransformedSourceName( sourceName );

				SourceAndConverter< ? > croppedSourceAndConverter = SourceCropper.crop( sourceAndConverter, transformedSourceName, new FinalRealInterval( min, max ), centerAtOrigin );

				// store result
				sourceNameToSourceAndConverter.put( croppedSourceAndConverter.getSpimSource().getName(), croppedSourceAndConverter );
			}
		}
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}


	public static int[] getNumVoxels( double smallestVoxelSize, RealInterval interval )
	{
		int[] numVoxels = new int[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			numVoxels[ d ] = (int) Math.ceil( ( interval.realMax( d ) - interval.realMin( d ) ) / smallestVoxelSize );
		}
		return numVoxels;
	}

	private String getTransformedSourceName( String inputSourceName )
	{
		if ( sourceNamesAfterTransform != null )
		{
			return sourceNamesAfterTransform.get( this.sources.indexOf( inputSourceName ) );
		}
		else
		{
			return inputSourceName;
		}
	}
}
