package org.embl.mobie.viewer.annotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AnnotatedIntervalAdapter< T extends AnnotatedInterval >
{
	class TimepointAndAnnotationId
	{
		private final String annotationId;
		private final int timepoint;

		public TimepointAndAnnotationId( int timepoint, String annotationId )
		{
			this.timepoint = timepoint;
			this.annotationId = annotationId;
		}

		public TimepointAndAnnotationId( AnnotatedInterval annotatedInterval )
		{
			this.timepoint = annotatedInterval.getTimepoint();
			this.annotationId = annotatedInterval.getName();
		}

		@Override
		public boolean equals( Object o )
		{
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;
			TimepointAndAnnotationId that = ( TimepointAndAnnotationId ) o;
			return Integer.compare( this.timepoint, that.getTimepoint() ) == 0 &&
					Objects.equals( this.annotationId, that.getAnnotationId() );
		}

		@Override
		public int hashCode()
		{
			return Objects.hash( annotationId, timepoint );
		}

		public String getAnnotationId()
		{
			return annotationId;
		}

		public int getTimepoint()
		{
			return timepoint;
		}
	}


	private HashMap< TimepointAndAnnotationId, T > timepointAndAnnotationToObject;

	/**
	 * For lazy initialization
	 */
	public AnnotatedIntervalAdapter()
	{
		timepointAndAnnotationToObject = new HashMap<>();
	}

	public AnnotatedIntervalAdapter( List< T > objects )
	{
		timepointAndAnnotationToObject = new HashMap<>();

		for ( T object : objects )
			timepointAndAnnotationToObject.put( new TimepointAndAnnotationId( object ), object );
	}

	public T getAnnotatedInterval( int timepoint, String annotationId )
	{
		final TimepointAndAnnotationId timepointAndAnnotationId = new TimepointAndAnnotationId( timepoint, annotationId  );

		return getAnnotatedInterval( timepointAndAnnotationId );
	}

	private T getAnnotatedInterval( TimepointAndAnnotationId timepointAndAnnotationId )
	{
		return timepointAndAnnotationToObject.get( timepointAndAnnotationId );
	}

	// deserialize
	public List< T > getAnnotatedIntervals( List< String > strings )
	{
		final ArrayList< T > annotatedIntervals = new ArrayList<>();
		for ( String string : strings )
		{
			final String[] split = string.split( ";" );
			final TimepointAndAnnotationId timepointAndAnnotationId = new TimepointAndAnnotationId( Integer.parseInt( split[1] ), split[0] );
			annotatedIntervals.add( getAnnotatedInterval( timepointAndAnnotationId ) );
		}
		return annotatedIntervals;
	}
}
