package org.embl.mobie.viewer.annotation;

import bdv.viewer.Source;
import net.imglib2.RealInterval;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.ImageStore;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.SourceHelper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface ImageAnnotation extends Region, Annotation
{
	default RealMaskRealInterval getUnionMask( List< String > imageNames, int t )
	{
		final Set< Image< ? > > images = ImageStore.getImages( imageNames );

		// use below code once https://github.com/imglib/imglib2-roi/pull/63 is merged
//		RealMaskRealInterval union = null;
//		for ( Image< ? > image : images )
//		{
//			final RealMaskRealInterval mask = image.getBounds( t );
//
//			if ( union == null )
//			{
//				union = mask;
//			}
//			else
//			{
//				if ( Intervals.equals( mask, union ) )
//					continue;
//				union = union.or( mask );
//			}
//		}
//		return union;

		RealInterval union = null;
		for ( Image< ? > image : images )
		{
			final RealInterval mask =  image.getBounds( t );

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.equals( mask, union ) )
					continue;
				union = Intervals.union( mask, union );
			}
		}

		return GeomMasks.closedBox( union.minAsDoubleArray(), union.maxAsDoubleArray() );
	}
}
