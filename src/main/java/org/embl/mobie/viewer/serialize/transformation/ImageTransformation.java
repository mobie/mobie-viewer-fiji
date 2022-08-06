package org.embl.mobie.viewer.serialize.transformation;

public interface ImageTransformation< A, B > extends Transformation
{
	String getTransformedImageName( String imageName );
}
