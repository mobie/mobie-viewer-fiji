package org.embl.mobie.viewer.source;

import de.embl.cba.tables.imagesegment.ImageSegment;

public class SegmentType< I extends ImageSegment > extends GenericType< I >
{
	public SegmentType()
	{
	}

	public SegmentType( I segment )
	{
		super( segment );
	}

	@Override
	public GenericType< I > createVariable()
	{
		return new SegmentType<>();
	}

	@Override
	public GenericType< I > copy()
	{
		return new SegmentType<>( object );
	}

	@Override
	public void set( GenericType< I > genericType )
	{
		object = genericType.get();
	}

	@Override
	public boolean valueEquals( GenericType< I > genericType )
	{
		return object.equals( genericType.get() );
	}
}
