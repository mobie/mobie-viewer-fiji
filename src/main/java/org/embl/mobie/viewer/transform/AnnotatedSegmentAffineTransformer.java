package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.SegmentAnnotation;

public class AnnotatedSegmentAffineTransformer implements AnnotationTransformer< SegmentAnnotation, AffineTransformedSegmentAnnotation >
{
	private AffineTransform3D transformation;

	public AnnotatedSegmentAffineTransformer( AffineTransform3D affineTransform3D )
	{
		this.transformation = affineTransform3D;
	}

	@Override
	public AffineTransformedSegmentAnnotation transform( SegmentAnnotation segmentAnnotation )
	{
		return new AffineTransformedSegmentAnnotation( segmentAnnotation, transformation );
	}
}
