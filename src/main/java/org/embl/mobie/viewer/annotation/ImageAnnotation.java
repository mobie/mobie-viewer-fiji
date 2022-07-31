package org.embl.mobie.viewer.annotation;

import bdv.viewer.Source;
import net.imglib2.roi.RealMaskRealInterval;
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
		final Set< ? extends Image< ? > > images = imageNames.stream().map( name -> ImageStore.images.get( name ) ).collect( Collectors.toSet() );

		RealMaskRealInterval union = null;
		for ( Image< ? > image : images )
		{
			final Source< ? > source = image.getSourcePair().getSource();
			final RealMaskRealInterval mask = SourceHelper.getMask( source, t );

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.equals( mask, union ) )
					continue;
				union = union.or( mask );
			}
		}

		return union;
	}
}
