package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RealInterval;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

public class SourceCropper
{
	public static SourceAndConverter< ? > crop( SourceAndConverter< ? > sourceAndConverter, String transformedSourceName, RealInterval interval, boolean centerAtOrigin )
	{
		// determine number of voxels for resampling
		// TODO the current method may over-sample quite a bit
		final double smallestVoxelSize = getSmallestVoxelSize( sourceAndConverter );
		final FinalVoxelDimensions croppedSourceVoxelDimensions = new FinalVoxelDimensions( sourceAndConverter.getSpimSource().getVoxelDimensions().unit(), smallestVoxelSize, smallestVoxelSize, smallestVoxelSize );
		int[] numVoxels = CropSourceTransformer.getNumVoxels( smallestVoxelSize, interval );
		SourceAndConverter< ? > cropModel = new EmptySourceAndConverterCreator("Model", interval, numVoxels[ 0 ], numVoxels[ 1 ], numVoxels[ 2 ], croppedSourceVoxelDimensions ).get();

		// resample generative source as model source
		SourceAndConverter< ? > croppedSourceAndConverter = new SourceResampler( sourceAndConverter, cropModel, transformedSourceName, false,false, false,0).get();

		if ( centerAtOrigin )
			croppedSourceAndConverter = TransformHelper.centerAtOrigin( croppedSourceAndConverter );

		return croppedSourceAndConverter;
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
}
