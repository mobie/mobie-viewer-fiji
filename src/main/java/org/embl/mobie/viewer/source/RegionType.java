package org.embl.mobie.viewer.source;

import org.embl.mobie.viewer.annotate.Region;

public class RegionType< R extends Region > extends AnnotationType< R >
{
	public RegionType()
	{
	}

	public RegionType( R region )
	{
		super( region );
	}

	@Override
	public AnnotationType< R > createVariable()
	{
		return new RegionType<>();
	}

	@Override
	public AnnotationType< R > copy()
	{
		return new RegionType<>( annotation );
	}

	@Override
	public void set( AnnotationType< R > annotationType )
	{
		this.annotation = annotationType.get();
	}

	@Override
	public boolean valueEquals( AnnotationType< R > annotationType )
	{
		return annotation.equals( annotationType.get() );
	}
}
