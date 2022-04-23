package org.embl.mobie.viewer.annotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AnnotatedMaskAdapter
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

		public TimepointAndAnnotationId( AnnotatedMask annotatedMask )
		{
			this.timepoint = annotatedMask.getTimepoint();
			this.annotationId = annotatedMask.getName();
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

	class TimePointAndLabel
	{
		private final double label;
		private final int timepoint;

		public TimePointAndLabel( int timepoint, double label )
		{
			this.timepoint = timepoint;
			this.label = label;
		}

		@Override
		public boolean equals( Object o )
		{
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;
			TimePointAndLabel that = ( TimePointAndLabel ) o;
			return this.timepoint == that.getTimepoint() &&
					Objects.equals( this.label, that.getLabel() );
		}

		@Override
		public int hashCode()
		{
			return Objects.hash( timepoint, label );
		}

		public double getLabel()
		{
			return label;
		}

		public int getTimepoint()
		{
			return timepoint;
		}
	}


	private HashMap< TimepointAndAnnotationId, AnnotatedMaskTableRow > timepointAndAnnotationToObject;
	private HashMap< TimePointAndLabel, AnnotatedMaskTableRow > timePointAndLabelToObject;

	/**
	 * For lazy initialization
	 */
	public AnnotatedMaskAdapter()
	{
		timepointAndAnnotationToObject = new HashMap<>();
	}

	public AnnotatedMaskAdapter( List< AnnotatedMaskTableRow > tableRows )
	{
		timepointAndAnnotationToObject = new HashMap<>();
		timePointAndLabelToObject = new HashMap<>();

		final int numTableRows = tableRows.size();
		for ( int rowIndex = 0; rowIndex < numTableRows; rowIndex++ )
		{
			final AnnotatedMaskTableRow annotatedMaskTableRow = tableRows.get( rowIndex );
			timepointAndAnnotationToObject.put( new TimepointAndAnnotationId( annotatedMaskTableRow ), annotatedMaskTableRow );
			timePointAndLabelToObject.put( new TimePointAndLabel( annotatedMaskTableRow.getTimepoint(), rowIndex ), annotatedMaskTableRow );
		}
	}

	public AnnotatedMaskTableRow getAnnotatedMask( int timepoint, String annotationId )
	{
		final TimepointAndAnnotationId timepointAndAnnotationId = new TimepointAndAnnotationId( timepoint, annotationId  );

		return getAnnotatedMask( timepointAndAnnotationId );
	}

	public AnnotatedMaskTableRow getAnnotatedMask( int timepoint, double label )
	{
		final TimePointAndLabel timepointAndAnnotationId = new TimePointAndLabel( timepoint, label  );

		return getAnnotatedMask( timepointAndAnnotationId );
	}

	private AnnotatedMaskTableRow getAnnotatedMask( TimepointAndAnnotationId timepointAndAnnotationId )
	{
		return timepointAndAnnotationToObject.get( timepointAndAnnotationId );
	}

	private AnnotatedMaskTableRow getAnnotatedMask( TimePointAndLabel timePointAndLabel )
	{
		return timePointAndLabelToObject.get( timePointAndLabel );
	}

	// deserialize
	public List< AnnotatedMaskTableRow > getAnnotatedIntervals( List< String > strings )
	{
		final ArrayList< AnnotatedMaskTableRow > annotatedMasks = new ArrayList<>();
		for ( String string : strings )
		{
			final String[] split = string.split( ";" );
			final TimepointAndAnnotationId timepointAndAnnotationId = new TimepointAndAnnotationId( Integer.parseInt( split[0] ), split[1] );
			annotatedMasks.add( getAnnotatedMask( timepointAndAnnotationId ) );
		}
		return annotatedMasks;
	}
}
