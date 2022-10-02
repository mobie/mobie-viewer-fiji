package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.annotation.Annotation;

public class AnnotationAffineTransformer< A extends Annotation, TA extends A > implements AnnotationTransformer< A, TA >
{
	private AffineTransform3D affineTransform3D;

	public AnnotationAffineTransformer( AffineTransform3D affineTransform3D )
	{
		this.affineTransform3D = affineTransform3D;
	}

	@Override
	public TA transform( A annotation )
	{
		if ( annotation instanceof AnnotatedSegment )
		{
			final AffineTransformedAnnotatedSegment affineTransformedAnnotatedSegment = new AffineTransformedAnnotatedSegment( ( AnnotatedSegment ) annotation, affineTransform3D );

			return ( TA ) affineTransformedAnnotatedSegment;
		}
		else
		{
			throw new UnsupportedOperationException( "Affine transformation of " + annotation.getClass().getName() + " is currently not supported" );
		}
	}
}
