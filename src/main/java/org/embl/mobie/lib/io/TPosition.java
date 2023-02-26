package org.embl.mobie.lib.io;

public class TPosition
{
	private final String timepoint;

	public TPosition( String timepoint )
	{
		this.timepoint = timepoint;
	}

	@Override
	public String toString()
	{
		return timepoint;
	}
}
