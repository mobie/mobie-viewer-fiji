package de.embl.cba.mobie.transform;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.playground.SourceAffineTransformer;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CropSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	protected double[] min;
	protected double[] max;
	protected List< String > sources;
	protected List< String > sourceNamesAfterTransform;

	protected boolean shiftToOrigin = true;

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sourceAndConverters )
	{
		final ArrayList< SourceAndConverter< T > > transformedSources = new ArrayList<>( sourceAndConverters );

		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			final String inputSourceName = sourceAndConverter.getSpimSource().getName();

			if ( this.sources.contains( inputSourceName ) )
			{
				String transformedSourceName = getTransformedSourceName( inputSourceName );

				// determine number of voxels for resampling
				// the current method may over-sample quite a bit
				final double smallestVoxelSize = getSmallestVoxelSize( sourceAndConverter );
				final FinalVoxelDimensions croppedSourceVoxelDimensions = new FinalVoxelDimensions( sourceAndConverter.getSpimSource().getVoxelDimensions().unit(), smallestVoxelSize, smallestVoxelSize, smallestVoxelSize );
				int[] numVoxels = getNumVoxels( smallestVoxelSize );
				SourceAndConverter< ? > cropModel = new EmptySourceAndConverterCreator("Model", new FinalRealInterval( min, max ), numVoxels[ 0 ], numVoxels[ 1 ], numVoxels[ 2 ], croppedSourceVoxelDimensions ).get();

				// Resample generative source as model source
				SourceAndConverter< T > croppedSourceAndConverter =
						new SourceResampler( sourceAndConverter, cropModel, transformedSourceName, false,false, false,0).get();

				final VoxelDimensions voxelDimensions = croppedSourceAndConverter.getSpimSource().getVoxelDimensions();

				if ( shiftToOrigin )
				{
					croppedSourceAndConverter = shiftToOrigin( croppedSourceAndConverter );
				}

				// replace the source in the list
				transformedSources.remove( sourceAndConverter );
				transformedSources.add( croppedSourceAndConverter );

				// store translation
				final AffineTransform3D transform3D = new AffineTransform3D();
				if ( shiftToOrigin == true )
				{
					transform3D.translate( Arrays.stream( min ).map( x -> -x ).toArray() );
				}
				sourceNameToTransform.put( transformedSourceName, transform3D );
			}
		}

		return transformedSources;
	}

	public static < T extends NumericType< T > > SourceAndConverter< T > shiftToOrigin( SourceAndConverter< T > sourceAndConverter )
	{
		final AffineTransform3D translate = new AffineTransform3D();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );
		final FinalRealInterval bounds = Utils.estimateBounds( sourceAndConverter.getSpimSource() );
		final double[] min = bounds.minAsDoubleArray();
		translate.translate( min );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( translate.inverse() );
		sourceAndConverter = transformer.apply( sourceAndConverter );
		return sourceAndConverter;
	}

	private int[] getNumVoxels( double smallestVoxelSize )
	{
		int[] numVoxels = new int[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			numVoxels[ d ] = (int) Math.ceil( ( max[ d ] - min[ d ] ) / smallestVoxelSize );
		}
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
