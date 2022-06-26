package org.embl.mobie.viewer.source;

import de.embl.cba.tables.imagesegment.ImageSegment;

public class SegmentAnnotationType< I extends ImageSegment > extends AnnotationType< I >
{
	public SegmentAnnotationType()
	{
	}

	public SegmentAnnotationType( I segment )
	{
		super( segment );
	}

	@Override
	public AnnotationType< I > createVariable()
	{
		return new SegmentAnnotationType<>();
	}

	@Override
	public AnnotationType< I > copy()
	{
		return new SegmentAnnotationType<>( annotation );
	}

	@Override
	public void set( AnnotationType< I > annotationType )
	{
		annotation = annotationType.getAnnotation();
	}

	@Override
	public boolean valueEquals( AnnotationType< I > annotationType )
	{
		return annotation.equals( annotationType.getAnnotation() );
	}
}
