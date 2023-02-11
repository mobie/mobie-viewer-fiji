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

	protected void searchNode( final KDTreeNode< T > current, final double[] radii )
	{
		boolean closeEnough = true;
		double sum = 0;
		for ( int d = 0; d < n; ++d )
		{
			if ( Math.abs( pos[ d ] - current.getFloatPosition( d ) ) > radii[ d ] )
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
			searchNode( nearChild, radii );

		// search the away branch - maybe
		if ( ( axisAbsDistance <= radii[ current.getSplitDimension() ] ) && ( awayChild != null ) )
			searchNode( awayChild, radii );
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

