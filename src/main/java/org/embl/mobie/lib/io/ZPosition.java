package org.embl.mobie.lib.io;

public class ZPosition
{
	private final String timepoint;

	public ZPosition( String timepoint )
	{
		this.timepoint = timepoint;
	}

	@Override
	public String toString()
	{
		return timepoint;
	}
}
