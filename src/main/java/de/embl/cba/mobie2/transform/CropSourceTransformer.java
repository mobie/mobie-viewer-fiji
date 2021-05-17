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
	private double[] min;
	private double[] max;
	private boolean shiftToOrigin = true;

	@Override
	public List< SourceAndConverter< T > > transform( List< SourceAndConverter< T > > sources )
	{
		final ArrayList< SourceAndConverter< T > > transformedSources = new ArrayList<>( sources );

		for ( SourceAndConverter< ? > source : sources )
		{
			final String name = source.getSpimSource().getName();

			if ( this.sources.contains( name ) )
			{
				// transform, i.e. crop
				final SourceAndConverter< T > transformedSource = new SourceCropper( source, name, new FinalRealInterval( min, max ), shiftToOrigin ).get();

				// replace the source in the list
				transformedSources.remove( source );
				transformedSources.add( transformedSource );

				// store translation
				final AffineTransform3D transform3D = new AffineTransform3D();
				if ( shiftToOrigin == true )
				{
					transform3D.translate( Arrays.stream( min ).map( x -> -x ).toArray() );
					sourceNameToTransform.put( name, transform3D );
				}
				else
				{
					sourceNameToTransform.put( name, transform3D );
				}
			}
		}

		return transformedSources;
	}
}
