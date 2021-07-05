package de.embl.cba.mobie.transform;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.playground.SourceChanger;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

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

				// transform, i.e. crop
//				final SourceAndConverter< T > transformedSourceOld = new SourceCropper( sourceAndConverter, transformedSourceName, new FinalRealInterval( min, max ), shiftToOrigin ).get();
//
//				final CroppedSource< T > croppedSource = ( CroppedSource< T > ) new CroppedSource( sourceAndConverter.getSpimSource(), transformedSourceName, new FinalRealInterval( min, max ), shiftToOrigin );

				final SourceAndConverter< T > croppedSourceAndConverter = new SourceChanger( ( Function< Source< ? >, Source< ? > > ) source -> new CroppedSource( source, transformedSourceName, new FinalRealInterval( min, max ), shiftToOrigin  ) ).apply( sourceAndConverter );

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
