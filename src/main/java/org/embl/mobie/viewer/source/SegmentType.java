package org.embl.mobie.viewer.source;

import de.embl.cba.tables.imagesegment.ImageSegment;

public class SegmentType< I extends ImageSegment > extends AnnotationType< I >
{
	private I segment;

	public SegmentType()
	{
		this(  null);
	}

	public SegmentType( I segment )
	{
		this.segment = segment;
	}

	@Override
	public AnnotationType< I > createVariable()
	{
		return new SegmentType<>();
	}

	@Override
	public AnnotationType< I > copy()
	{
		return new SegmentType<>( segment );
	}

	@Override
	public void set( AnnotationType< I > c )
	{
		segment = c.getAnnotation();
	}

	@Override
	public boolean valueEquals( AnnotationType< I > annotationType )
	{
		return segment.equals( annotationType.getAnnotation() );
	}

	@Override
	public I getAnnotation()
	{
		return segment;
	}
}
