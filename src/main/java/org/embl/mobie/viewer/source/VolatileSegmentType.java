package org.embl.mobie.viewer.source;

import de.embl.cba.tables.imagesegment.ImageSegment;

public class VolatileSegmentType< I extends ImageSegment > extends VolatileAnnotationType< I >
{
	private I segment;

	public VolatileSegmentType()
	{
		this(  null,true );
	}

	public VolatileSegmentType( I segment, boolean valid )
	{
		super( segment, valid );
		this.segment = segment;
	}

	public VolatileSegmentType( I segment )
	{
		super( segment, true );
		this.segment = segment;
	}

	@Override
	public VolatileAnnotationType< I > createVariable()
	{
		return new VolatileSegmentType<>();
	}

	@Override
	public VolatileAnnotationType< I > copy()
	{
		return new VolatileSegmentType<>( segment, valid );
	}

	@Override
	public void set( VolatileAnnotationType< I > c )
	{
		segment = c.get();
	}

	@Override
	public boolean valueEquals( VolatileAnnotationType< I > annotationType )
	{
		return segment.equals( annotationType.get() );
	}
}
