package de.embl.cba.platynereis.utils;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.platybrowser.PlatyViews;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.concurrent.ExecutionException;


/**
 * TODO: probably move to bdv-utils
 *
 *
 */
public abstract class BdvViewChanger
{

	public static PlatyViews views = new PlatyViews();

	public static void moveToView( Bdv bdv, String view )
	{
		double[] doubles = getDoubles( view );

		moveToDoubles( bdv, doubles );
	}

	public static void moveToDoubles( Bdv bdv, double[] doubles )
	{
		if ( doubles.length == 3 )
		{
			BdvUtils.zoomToPosition( bdv, asPosition4D( doubles ), 15D, 1000 );
		}
		else if ( doubles.length == 4 )
		{
			BdvUtils.zoomToPosition( bdv, doubles, 15D, 1000 );
		}
		else if ( doubles.length == 12 )
		{
			BdvUtils.changeBdvViewerTransform( bdv, asView( doubles ), 1000  );
		}
		else
		{
			Utils.log( "Cannot parse view string :-("  );
		}
	}

	public static AffineTransform3D asView( double[] doubles )
	{
		final AffineTransform3D view = new AffineTransform3D( );
		view.set( doubles );
		return view;
	}

	public static double[] asPosition4D( double[] doubles )
	{
		final double[] position4D = new double[ 4 ];
		for ( int d = 0; d < 3; d++ )
			position4D[ d ] = doubles[ d ];
		return position4D;
	}

	public static double[] getDoubles( String view )
	{

		if ( views.views().containsKey( view ) )
		{
			return views.views().get( view );
		}
		else if ( view.contains( "View" ) )
		{
			view = view.replace( "View: (", "" );
			view = view.replace( ")", "" );
			return Utils.delimitedStringToDoubleArray( view, "," );
		}
		else if ( view.contains( "Position" ) )
		{
			view = view.replace( "Position: (", "" );
			view = view.replace( ")", "" );
			return Utils.delimitedStringToDoubleArray( view, "," );
		}
		else
		{
			try
			{
				return Utils.delimitedStringToDoubleArray( view, "," );
			} catch ( Exception e )
			{
				Utils.log( "Cannot parse view string :-(" );
				return null;
			}
		}

	}
}
