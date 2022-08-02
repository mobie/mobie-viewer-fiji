package org.embl.mobie.viewer.transform.image;

import org.embl.mobie.viewer.source.Image;

import java.util.Collection;
import java.util.List;

public interface StitchingTransformation< T > extends Transformation
{
	Image< T > apply( List< Image< T > > images );
}
