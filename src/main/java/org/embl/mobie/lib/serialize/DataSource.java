package org.embl.mobie.lib.serialize;

public interface DataSource
{
	String getName();
	void setName( String name );
	void preInit( boolean preInit );
	boolean preInit();
}
