package org.embl.mobie3.viewer.transform;

import org.embl.mobie3.viewer.annotation.AnnotatedSegment;

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
