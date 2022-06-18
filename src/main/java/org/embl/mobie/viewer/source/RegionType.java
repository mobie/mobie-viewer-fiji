package org.embl.mobie.viewer.source;

import org.embl.mobie.viewer.annotate.Region;

public class RegionType< R extends Region > extends AnnotationType< R >
{
	private R region;

	public RegionType( R region )
	{
		this.region = region;
	}

	public RegionType()
	{
		this.region = null;
	}

	@Override
	public AnnotationType< R > createVariable()
	{
		return new RegionType<>();
	}

	@Override
	public AnnotationType< R > copy()
	{
		return new RegionType<>( region );
	}

	@Override
	public void set( AnnotationType< R > annotationType )
	{
		this.region = annotationType.getAnnotation();
	}

	@Override
	public boolean valueEquals( AnnotationType< R > annotationType )
	{
		return region.equals( annotationType.getAnnotation() );
	}

	@Override
	public R getAnnotation()
	{
		return region;
	}
}
