package org.embl.mobie.lib.serialize;

public abstract class AbstractDataSource implements DataSource
{
	// Serialisation

	// Runtime

	protected transient String name;

	protected transient boolean preInit;

	public String getName(){ return name; };

	public void setName( String name ){ this.name = name; };

	public void preInit( boolean preInit ){ this.preInit = preInit; };

	public boolean preInit(){ return preInit; };

	public AbstractDataSource()
	{
		this.name = "dataSource";
	}

	public AbstractDataSource( String name )
	{
		this.name = name;
	}
}
