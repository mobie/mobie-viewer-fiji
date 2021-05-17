package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CropSourceTransformer< T extends NumericType< T > > extends AbstractSourceTransformer< T >
{
	private List< String > sources;
	private List< String > names;
	private double[] min;
	private double[] max;
	private boolean shiftToOrigin = true;

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

				// transform, i.e. crop
				final SourceAndConverter< T > transformedSource = new SourceCropper( sourceAndConverter, transformedSourceName, new FinalRealInterval( min, max ), shiftToOrigin ).get();

				// replace the source in the list
				transformedSources.remove( sourceAndConverter );
				transformedSources.add( transformedSource );

				// store translation
				final AffineTransform3D transform3D = new AffineTransform3D();
				if ( shiftToOrigin == true )
				{
					transform3D.translate( Arrays.stream( min ).map( x -> -x ).toArray() );
					sourceNameToTransform.put( transformedSourceName, transform3D );
				}
				else
				{
					sourceNameToTransform.put( transformedSourceName, transform3D );
				}
			}
		}

		return transformedSources;
	}

	private String getTransformedSourceName( String inputSourceName )
	{
		String transformedSourceName;
		if ( names != null )
		{
			transformedSourceName = names.get( this.sources.indexOf( inputSourceName ) );
		}
		else
		{
			transformedSourceName = inputSourceName;
		}
		return transformedSourceName;
	}
}
