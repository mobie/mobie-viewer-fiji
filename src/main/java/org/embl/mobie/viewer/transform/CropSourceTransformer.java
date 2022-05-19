package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

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

				// determine number of voxels for resampling
				// the current method may over-sample quite a bit
				final double smallestVoxelSize = getSmallestVoxelSize( sourceAndConverter );
				// slightly enlarge the crop
				// important to deal with quasi 2D images or crops
				final double[] minMinusVoxelSize = new double[ 3 ];
				final double[] maxPlusVoxelSize = new double[ 3 ];
				for ( int d = 0; d < 3; d++ )
				{
					minMinusVoxelSize[ d ] = min[ d ] - smallestVoxelSize;
					maxPlusVoxelSize[ d ] = max[ d ] + smallestVoxelSize;
				}
				final FinalVoxelDimensions croppedSourceVoxelDimensions = new FinalVoxelDimensions( sourceAndConverter.getSpimSource().getVoxelDimensions().unit(), smallestVoxelSize, smallestVoxelSize, smallestVoxelSize );
				int[] numVoxels = getNumVoxels( smallestVoxelSize, maxPlusVoxelSize, minMinusVoxelSize );
				SourceAndConverter< ? > cropModel = new EmptySourceAndConverterCreator("Model", new FinalRealInterval( minMinusVoxelSize, maxPlusVoxelSize ), numVoxels[ 0 ], numVoxels[ 1 ], numVoxels[ 2 ], croppedSourceVoxelDimensions ).get();

				// resample generative source as model source
				SourceAndConverter< ? > croppedSourceAndConverter = new SourceResampler( sourceAndConverter, cropModel, transformedSourceName, false,false, false,0).get();

				if ( centerAtOrigin )
				{
					croppedSourceAndConverter = TransformHelper.centerAtOrigin( croppedSourceAndConverter );
				}

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

	private int[] getNumVoxels( double smallestVoxelSize, double[] max, double[] min )
	{
		int[] numVoxels = new int[ 3 ];
		for ( int d = 0; d < 3; d++ )
			numVoxels[ d ] = Math.max ( (int) Math.ceil( ( max[ d ] - min[ d ] ) / smallestVoxelSize ), 1 );

		return numVoxels;
	}

	public static double getSmallestVoxelSize( SourceAndConverter< ? > sourceAndConverter )
	{
		final VoxelDimensions voxelDimensions = sourceAndConverter.getSpimSource().getVoxelDimensions();
		double smallestVoxelSize = Double.MAX_VALUE;
		for ( int d = 0; d < 3; d++ )
		{
			if ( voxelDimensions.dimension( d ) < smallestVoxelSize )
			{
				smallestVoxelSize = voxelDimensions.dimension( d );
			}
		}
		return smallestVoxelSize;
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
