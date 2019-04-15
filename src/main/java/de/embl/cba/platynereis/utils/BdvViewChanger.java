package de.embl.cba.platynereis.utils;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.platybrowser.PlatyViews;
import net.imglib2.realtransform.AffineTransform3D;


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
			final double[] position4D = new double[ 4 ];
			for ( int d = 0; d < 4; d++ )
				position4D[ d ] = doubles[ d ];
			BdvUtils.zoomToPosition( bdv, position4D, 15, 1000 );
			return;
		}
		else if ( doubles.length == 12 )
		{
			final AffineTransform3D view = new AffineTransform3D();
			view.set( Utils.delimitedStringToDoubleArray( view, "," ) );
			BdvUtils.changeBdvViewerTransform( bdv, view, 1000  );
		}
		else
		{
			Utils.log( "Cannot parse view string :-("  );
		}
	}

	public static double[] getDoubles( String view )
	{
		double[] doubles;
		if ( views.views().containsKey( view ) )
		{
			doubles = views.views().get( view );
		}
		else if ( view.contains( "View" ) )
		{
			view = view.replace( "View: (", "" );
			view = view.replace( ")", "" );
			doubles = Utils.delimitedStringToDoubleArray( view, "," );
		}
		else if ( view.contains( "Position" ) )
		{
			view = view.replace( "Position: (", "" );
			view = view.replace( ")", "" );
			doubles = Utils.delimitedStringToDoubleArray( view, "," );
		}
		return doubles;
	}
}
