package org.embl.mobie.viewer.source;

import de.embl.cba.tables.imagesegment.ImageSegment;

public class SegmentGenericType< I extends ImageSegment > extends GenericType< I >
{
	public SegmentGenericType()
	{
	}

	public SegmentGenericType( I segment )
	{
		super( segment );
	}

	@Override
	public GenericType< I > createVariable()
	{
		return new SegmentGenericType<>();
	}

	@Override
	public GenericType< I > copy()
	{
		return new SegmentGenericType<>( object );
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
