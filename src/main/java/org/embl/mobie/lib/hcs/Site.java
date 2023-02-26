package org.embl.mobie.lib.hcs;

public class Site
{
	private final String name;

	private final SiteStorageLocation storageLocation = new SiteStorageLocation();

	public Site( String name )
	{
		this.name = name;
	}

	public SiteStorageLocation storageLocation()
	{
		return storageLocation;
	}

	public String getName()
	{
		return name;
	}
}
