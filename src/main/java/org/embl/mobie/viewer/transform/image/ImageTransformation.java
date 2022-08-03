package org.embl.mobie.viewer.transform.image;

import org.embl.mobie.viewer.source.Image;

public interface ImageTransformation< A, B > extends Transformation
{
	String getTransformedImageName( String imageName );
}
