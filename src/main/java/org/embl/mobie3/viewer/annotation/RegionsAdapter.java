/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie3.viewer.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RegionsAdapter
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

		public TimepointAndAnnotationId( Region region )
		{
			this.timepoint = region.timePoint();
			this.annotationId = region.regionId();
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


	private HashMap< TimepointAndAnnotationId, RegionTableRow > timepointAndAnnotationToObject;
	private HashMap< TimePointAndLabel, RegionTableRow > timePointAndLabelToObject;

	/**
	 * For lazy initialization
	 */
	public RegionsAdapter()
	{
		timepointAndAnnotationToObject = new HashMap<>();
	}

	public RegionsAdapter( List< RegionTableRow > tableRows )
	{
		timepointAndAnnotationToObject = new HashMap<>();
		timePointAndLabelToObject = new HashMap<>();

		final int numTableRows = tableRows.size();
		for ( int rowIndex = 0; rowIndex < numTableRows; rowIndex++ )
		{
			final RegionTableRow regionTableRow = tableRows.get( rowIndex );
			timepointAndAnnotationToObject.put( new TimepointAndAnnotationId( regionTableRow ), regionTableRow );
			timePointAndLabelToObject.put( new TimePointAndLabel( regionTableRow.timePoint(), rowIndex ), regionTableRow );
		}
	}

	public RegionTableRow getAnnotatedMask( int timepoint, String annotationId )
	{
		final TimepointAndAnnotationId timepointAndAnnotationId = new TimepointAndAnnotationId( timepoint, annotationId  );

		return getAnnotatedMask( timepointAndAnnotationId );
	}

	public RegionTableRow getAnnotatedMask( int timepoint, double label )
	{
		final TimePointAndLabel timepointAndAnnotationId = new TimePointAndLabel( timepoint, label  );

		return getAnnotatedMask( timepointAndAnnotationId );
	}

	private RegionTableRow getAnnotatedMask( TimepointAndAnnotationId timepointAndAnnotationId )
	{
		return timepointAndAnnotationToObject.get( timepointAndAnnotationId );
	}

	private RegionTableRow getAnnotatedMask( TimePointAndLabel timePointAndLabel )
	{
		return timePointAndLabelToObject.get( timePointAndLabel );
	}

	// deserialize
	public List< RegionTableRow > getAnnotatedMasks( List< String > strings )
	{
		final ArrayList< RegionTableRow > annotatedMasks = new ArrayList<>();
		for ( String string : strings )
		{
			final String[] split = string.split( ";" );
			final TimepointAndAnnotationId timepointAndAnnotationId = new TimepointAndAnnotationId( Integer.parseInt( split[0] ), split[1] );
			annotatedMasks.add( getAnnotatedMask( timepointAndAnnotationId ) );
		}
		return annotatedMasks;
	}
}
