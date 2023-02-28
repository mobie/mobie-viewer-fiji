package org.embl.mobie.lib.io;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TPosition implements Comparable< TPosition >
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

	@Override
	public boolean equals( Object o ) {
		// self check
		if (this == o)
			return true;
		// null check
		if (o == null)
			return false;
		// type check and cast
		if (getClass() != o.getClass())
			return false;
		TPosition tPosition = (TPosition) o;
		// field comparison
		return tPosition.timepoint.equals( timepoint );
	}

	@Override
	public int hashCode() {
		return Objects.hash( timepoint );
	}

	@Override
	public int compareTo( @NotNull TPosition o )
	{
		return this.toString().compareTo( o.toString() );
	}
}
