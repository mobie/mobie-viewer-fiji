package org.embl.mobie.viewer.transform;

import org.embl.mobie.viewer.annotation.SegmentAnnotation;
import org.embl.mobie.viewer.transform.image.Transformation;

public class AnnotatedSegmentTransformer implements AnnotationTransformer< SegmentAnnotation, AffineTransformedSegmentAnnotation >
{
	private Transformation transformation;

	public AnnotatedSegmentTransformer( Transformation transformation )
	{
		this.transformation = transformation;
	}

	@Override
	public AffineTransformedSegmentAnnotation transform( SegmentAnnotation segmentAnnotation )
	{
		return new AffineTransformedSegmentAnnotation( segmentAnnotation, transformation );
	}
}
