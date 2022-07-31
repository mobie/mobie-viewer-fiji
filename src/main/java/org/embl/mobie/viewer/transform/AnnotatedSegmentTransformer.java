package org.embl.mobie.viewer.transform;

import org.embl.mobie.viewer.annotation.SegmentAnnotation;
import org.embl.mobie.viewer.transform.image.Transformation;

public class AnnotatedSegmentTransformer implements AnnotationTransformer< SegmentAnnotation, TransformedSegmentAnnotation >
{
	private Transformation transformation;

	public AnnotatedSegmentTransformer( Transformation transformation )
	{
		this.transformation = transformation;
	}

	@Override
	public TransformedSegmentAnnotation transform( SegmentAnnotation segmentAnnotation )
	{
		return new TransformedSegmentAnnotation( segmentAnnotation, transformation );
	}
}
