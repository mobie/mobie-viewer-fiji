package de.embl.cba.mobie.transform;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Utils;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.util.Intervals;

import java.util.List;

public class TransformHelper
{
	public static List< SourceAndConverter< ? > > transformSourceAndConverters( List< SourceAndConverter< ? > > sourceAndConverters, List< SourceTransformer > sourceTransformers )
	{
		if ( sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : sourceTransformers )
			{
				sourceAndConverters = sourceTransformer.transform( sourceAndConverters );
				//break;
			}
		}

		return sourceAndConverters;
	}

	public static RealInterval unionRealInterval( List< ? extends Source< ? > > sources )
	{
		RealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final FinalRealInterval bounds = Utils.estimateBounds( source );

			if ( union == null )
			{
				union = bounds;
			}
			else
			{
				union = Intervals.union( bounds, union );
			}
		}

		return union;
	}
}
