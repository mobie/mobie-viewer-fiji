/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.plot;

import de.embl.cba.tables.Utils;
import org.embl.mobie.lib.annotation.Annotation;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Note: Turns out this does not really need to be a Supplier as
 * we only need one KDTree and not multiple, but it also does not
 * hurt, so I left it like this.
 *
 * @param <A> an annotation
 */
public class AnnotationKDTreeSupplier< A extends Annotation > implements Supplier< KDTree< A > >
{
	final private int numDimensions = 2; // for a 2-D scatter plot

	private ArrayList< RealPoint > locations;
	private ArrayList< A > annotations;
	private Map< A, RealPoint > annotationToCoordinate;
	double[] min = new double[ numDimensions ];
	double[] max = new double[ numDimensions ];
	private HashMap< String, Double > stringToNumber;

	public AnnotationKDTreeSupplier( Collection< A > inputData, String[] columns )
	{
		Arrays.fill( min, Double.MAX_VALUE );
		Arrays.fill( max, -Double.MAX_VALUE );

		initialiseDataPoints( inputData, columns );

		annotationToCoordinate = IntStream.range( 0, locations.size() ).boxed().collect( Collectors.toMap( i -> annotations.get( i ), i -> locations.get( i ) ) );
	}

	/**
	 * Create a KDTree, using copies of the tableRows and dataPoints,
	 * because the KDTree modifies those lists internally,
	 * which would lead to confusion and concurrency issues.
	 *
	 * @return KDTree
	 */
	@Override
	public KDTree< A > get()
	{
		final KDTree< A > kdTree = new KDTree<>( new ArrayList<>( annotations ), new ArrayList<>( locations ) );

		return kdTree;
	}

	private void initialiseDataPoints( Collection< A > inputData, String[] columns )
	{
		stringToNumber = new HashMap<>(); // in case we need to plot categorical columns
		locations = new ArrayList<>();
		annotations = new ArrayList<>( );

		Double[] coordinate = new Double[ numDimensions ];
		boolean isValidDataPoint;

		final Iterator< A > iterator = inputData.iterator();
		while( iterator.hasNext() )
		{
			final A annotation = iterator.next();

			isValidDataPoint = true;

			for ( int d = 0; d < numDimensions; d++ )
			{
				// TODO with the new table model we don't have to
				//  do the conversion to Double via String anymore.

				// TODO it would be convenient to be able to ask the annotation
				//   whether a feature is numeric or categorical

				Object value = annotation.getValue( columns[ d ] );
				if ( value == null )
				{
					// This can happen when merging tables
					// and not all rows have a match
					isValidDataPoint = false;
					break;
				}

				String cell = value.toString();
				try
				{
					coordinate[ d ] = Utils.parseDouble( cell );
				}
				catch ( Exception e )
				{
					if ( ! stringToNumber.containsKey( cell ) )
					{
						stringToNumber.put( cell, Double.valueOf( stringToNumber.size() ) );
					}

					coordinate[ d ] =  stringToNumber.get( cell );
				}

				if ( coordinate[ d ].isNaN() || coordinate[ d ].isInfinite() )
				{
					isValidDataPoint = false;
					break;
				}

				if ( coordinate[ d ] < min[ d ] ) min[ d ] = coordinate[ d ];
				if ( coordinate[ d ] > max[ d ] ) max[ d ] = coordinate[ d ];
			}

			if ( isValidDataPoint )
			{
				this.locations.add( new RealPoint( coordinate[ 0 ], coordinate[ 1 ] ) );
				this.annotations.add( annotation );
			}
		}

		if ( this.locations.size() == 0 )
			throw new UnsupportedOperationException( "Cannot create scatter plot, because there is no valid data point." );
	}

	public double[] getMin()
	{
		return min;
	}

	public double[] getMax()
	{
		return max;
	}

	public Map< A, RealPoint > getAnnotationToCoordinate()
	{
		return annotationToCoordinate;
	}
}
