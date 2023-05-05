/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.imglib2.KDTree;
import net.imglib2.KDTreeNode;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.util.ValuePair;

public class WithinDistancesSearchOnKDTree< T >
{
	protected KDTree< T > tree;

	protected final int n;

	protected final float[] pos;

	protected ArrayList< ValuePair< KDTreeNode< T >, Double > > resultPoints;

	public WithinDistancesSearchOnKDTree( final KDTree< T > tree )
	{
		this.tree = tree;
		this.n = tree.numDimensions();
		this.pos = new float[ n ];
		this.resultPoints = new ArrayList< ValuePair< KDTreeNode< T >, Double > >();
	}

	public void search( final RealLocalizable reference, final double[] distances, final boolean sortResults )
	{
		reference.localize( pos );
		resultPoints.clear();
		searchNode( tree.getRoot(), distances );
		if ( sortResults )
		{
			Collections.sort( resultPoints, new Comparator< ValuePair< KDTreeNode< T >, Double > >()
			{
				@Override
				public int compare( final ValuePair< KDTreeNode< T >, Double > o1, final ValuePair< KDTreeNode< T >, Double > o2 )
				{
					return Double.compare( o1.b, o2.b );
				}
			} );
		}
	}

	public int numDimensions()
	{
		return n;
	}

	protected void searchNode( final KDTreeNode< T > current, final double[] distances )
	{
		// for round spots:
		// https://math.stackexchange.com/questions/76457/check-if-a-point-is-within-an-ellipse
		boolean closeEnough = true;
		for ( int d = 0; d < n; ++d )
		{
			if ( Math.abs( pos[ d ] - current.getFloatPosition( d ) ) > distances[ d ] )
			{
				closeEnough = false;
				break;
			}
		}

		Double distance = Double.valueOf( 0 ); // TODO: If we want round dots and if we want to sort

		if ( closeEnough )
		{
			resultPoints.add( new ValuePair< KDTreeNode< T >, Double >( current, distance ) );
		}

		final double axisDiff = pos[ current.getSplitDimension() ] - current.getSplitCoordinate();
		final boolean leftIsNearBranch = axisDiff < 0;
		final double axisAbsDistance = Math.abs( axisDiff );

		// search the near branch
		final KDTreeNode< T > nearChild = leftIsNearBranch ? current.left : current.right;
		final KDTreeNode< T > awayChild = leftIsNearBranch ? current.right : current.left;
		if ( nearChild != null )
			searchNode( nearChild, distances );

		// search the away branch - maybe
		if ( ( axisAbsDistance <= distances[ current.getSplitDimension() ] ) && ( awayChild != null ) )
			searchNode( awayChild, distances );
	}

	public int numNeighbors()
	{
		return resultPoints.size();
	}

	public Sampler< T > getSampler( final int i )
	{
		return resultPoints.get( i ).a;
	}

	public RealLocalizable getPosition( final int i )
	{
		return resultPoints.get( i ).a;
	}

	public double getSquareDistance( final int i )
	{
		return resultPoints.get( i ).b;
	}

	public double getDistance( final int i )
	{
		return Math.sqrt( resultPoints.get( i ).b );
	}
}

