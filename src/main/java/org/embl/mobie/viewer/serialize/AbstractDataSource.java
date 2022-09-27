package org.embl.mobie.viewer.serialize;

public abstract class AbstractDataSource implements DataSource
{
	// Serialisation

	protected String name;

	// Runtime

	protected transient boolean preInit;

	public String getName(){ return name; };

	public void setName( String name ){ this.name = name; };

	public void preInit( boolean preInit ){ this.preInit = preInit; };

	public boolean preInit(){ return preInit; };
}
