package org.embl.mobie.viewer.transform.image;

import org.embl.mobie.viewer.source.Image;

import java.util.Collection;

public interface StitchingTransformation< T > extends Transformation
{
	Image< T > apply( Collection< Image< T > > images );
}
