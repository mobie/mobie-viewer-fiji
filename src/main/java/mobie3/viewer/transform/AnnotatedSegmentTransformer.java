package mobie3.viewer.transform;

import mobie3.viewer.annotation.AnnotatedSegment;

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
