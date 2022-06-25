package org.embl.mobie.viewer.source;

import org.embl.mobie.viewer.annotate.Region;

public class RegionType< R extends Region > extends GenericType< R >
{
	public RegionType()
	{
	}

	public RegionType( R region )
	{
		super( region );
	}

	@Override
	public GenericType< R > createVariable()
	{
		return new RegionType<>();
	}

	@Override
	public GenericType< R > copy()
	{
		return new RegionType<>( object );
	}

	@Override
	public void set( GenericType< R > genericType )
	{
		this.object = genericType.get();
	}

	@Override
	public boolean valueEquals( GenericType< R > genericType )
	{
		return object.equals( genericType.get() );
	}
}
