package org.embl.mobie.viewer.transform.image;

import org.embl.mobie.viewer.source.Image;

public interface ImageTransformation< A, B > extends Transformation
{
	Image< B > apply( Image< A > image );
	String getTransformedName( Image< A > image );
}
