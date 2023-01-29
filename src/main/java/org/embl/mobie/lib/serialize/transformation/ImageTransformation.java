package org.embl.mobie.lib.serialize.transformation;

public interface ImageTransformation< A, B > extends Transformation
{
	String getTransformedImageName( String imageName );
}
