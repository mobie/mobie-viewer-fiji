package org.embl.mobie.viewer.transform;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CropSourceTransformer< T extends NumericType< T >> extends AbstractSourceTransformer
{
	// Serialisation
	protected double[] min;
	protected double[] max;
	protected double[] affine; // from box to physical
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;
	protected boolean centerAtOrigin = true;

	public CropSourceTransformer( MaskedSource maskedSource )
	{
		min = maskedSource.getMaskInterval().minAsDoubleArray();
		max = maskedSource.getMaskInterval().maxAsDoubleArray();
		affine = maskedSource.getMaskToPhysicalTransform().getRowPackedCopy();
		sources = Arrays.asList( maskedSource.getWrappedSource().getName() );
		if ( ! maskedSource.getName().equals( maskedSource.getWrappedSource().getName() ))
			sourceNamesAfterTransform = Arrays.asList( maskedSource.getName() );
		centerAtOrigin = false;
	}

	@Override
	public void transform( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter )
	{
		AffineTransform3D transform = new AffineTransform3D() ;
		if ( affine != null )
			transform.set( affine );

		for ( String sourceName : sourceNameToSourceAndConverter.keySet() )
		{
			if ( sources.contains( sourceName ) )
			{
				final SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( sourceName );
				String transformedSourceName = getTransformedSourceName( sourceName );

				SourceAndConverter< ? > croppedSourceAndConverter;
//				if ( affine == null )
//					croppedSourceAndConverter = cropViaResampling( sourceAndConverter, transformedSourceName, new FinalRealInterval( min, max ), centerAtOrigin );
//				else // TODO: Below does not seem to work?!...check with Martin
					croppedSourceAndConverter = new SourceAndConverterCropper( sourceAndConverter, transformedSourceName, new FinalRealInterval( min, max ), transform ).get();

				if ( centerAtOrigin )
					croppedSourceAndConverter = TransformHelper.centerAtOrigin( croppedSourceAndConverter );

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

	private static SourceAndConverter< ? > cropViaResampling( SourceAndConverter< ? > sourceAndConverter, String transformedSourceName, RealInterval interval, boolean centerAtOrigin )
	{
		// determine number of voxels for resampling
		// TODO the current method may over-sample quite a bit
		final double smallestVoxelSize = getSmallestVoxelSize( sourceAndConverter );
		final FinalVoxelDimensions croppedSourceVoxelDimensions = new FinalVoxelDimensions( sourceAndConverter.getSpimSource().getVoxelDimensions().unit(), smallestVoxelSize, smallestVoxelSize, smallestVoxelSize );
		int[] numVoxels = getNumVoxels( smallestVoxelSize, interval );
		SourceAndConverter< ? > cropModel = new EmptySourceAndConverterCreator("Model", interval, numVoxels[ 0 ], numVoxels[ 1 ], numVoxels[ 2 ], croppedSourceVoxelDimensions ).get();

		// resample generative source as model source
		SourceAndConverter< ? > croppedSourceAndConverter = new SourceResampler( sourceAndConverter, cropModel, transformedSourceName, false,false, false,0).get();

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


