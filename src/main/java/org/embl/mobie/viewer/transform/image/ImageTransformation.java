package org.embl.mobie.viewer.transform.image;

public interface ImageTransformation< A, B > extends Transformation
{
	String getTransformedImageName( String imageName );
}
