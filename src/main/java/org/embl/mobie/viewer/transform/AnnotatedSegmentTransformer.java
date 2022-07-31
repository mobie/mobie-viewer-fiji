package org.embl.mobie.viewer.transform;

import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import org.embl.mobie.viewer.transform.image.Transformation;

public class AnnotatedSegmentTransformer implements AnnotationTransformer< AnnotatedSegment, TransformedAnnotatedSegment >
{
	private Transformation transformation;

	public AnnotatedSegmentTransformer( Transformation transformation )
	{
		this.transformation = transformation;
	}

	@Override
	public TransformedAnnotatedSegment transform( AnnotatedSegment annotatedSegment )
	{
		return new TransformedAnnotatedSegment( annotatedSegment, transformation );
	}
}
