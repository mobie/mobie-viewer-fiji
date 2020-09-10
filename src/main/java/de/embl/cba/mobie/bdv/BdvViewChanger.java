package de.embl.cba.mobie.bdv;

import bdv.util.*;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.utils.Utils;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * TODO: probably move to bdv-utils
 *
 *
 */
public abstract class BdvViewChanger
{
	public static int animationDurationMillis = 3000;

	private static BdvOverlaySource< BdvOverlay > pointOverlaySource;
	private static BdvPointOverlay bdvPointOverlay;
	private static boolean pointOverlaySourceIsActive;
	private static boolean isPointOverlayEnabled;

	public static void moveToView( Bdv bdv, String view )
	{
		double[] doubles = getDoubles( view );

		moveToDoubles( bdv, doubles );
	}

	public static void moveToNormalisedView( Bdv bdv, String view )
	{
		double[] doubles = getDoubles( view );

		if ( ! ( doubles.length == 12 ) )
		{
			throw new UnsupportedOperationException( "Please enter a comma separated list of 12 numbers." );
		}
		// TODO: "unnormalise" the transformation

		BdvUtils.changeBdvViewerTransform( bdv, asView( doubles ), animationDurationMillis );
	}

	public static void moveToDoubles( Bdv bdv, ArrayList< Double > doubles )
	{
		final double[] array =  new double[ doubles.size() ];
		for ( int i = 0; i < doubles.size(); i++ )
			array[ i ] = doubles.get( i );

		moveToDoubles( bdv, array );
	}

	/**
	 * TODO: The logic of just counting the number of doubles is fragile...
	 *
	 * @param bdv
	 * @param doubles
	 */
	public static void moveToDoubles( Bdv bdv, double[] doubles )
	{
		if ( doubles.length == 3 ) // 3D
		{
			BdvUtils.moveToPosition( bdv, doubles, 0, animationDurationMillis );

			if ( isPointOverlayEnabled )
				addPointOverlay( bdv, doubles );
		}
		else if ( doubles.length == 4 ) // 3D + t
		{
			final double[] position = new double[ 3 ];
			for ( int d = 0; d < 3; d++ )
				position[ d ] = doubles[ d ];

			BdvUtils.zoomToPosition( bdv, position, doubles[ 3 ], animationDurationMillis );
		}
		else if ( doubles.length == 12 ) // ViewerTransform
		{
			BdvUtils.changeBdvViewerTransform( bdv, asView( doubles ), animationDurationMillis );
		}
		else
		{
			Utils.log( "Cannot parse view string :-("  );
		}
	}

	public static void togglePointOverlay()
	{
		if ( pointOverlaySource == null ) return;

		pointOverlaySourceIsActive = ! pointOverlaySourceIsActive;
		pointOverlaySource.setActive( pointOverlaySourceIsActive );
	}

	public static void addPointOverlay( Bdv bdv, double[] doubles )
	{
		if ( bdvPointOverlay == null )
		{
			bdvPointOverlay = new BdvPointOverlay( doubles, 5.0 );
			pointOverlaySource = BdvFunctions.showOverlay(
					bdvPointOverlay,
					"point-overlay-" + Arrays.toString( doubles ),
					BdvOptions.options().addTo( bdv ) );
			pointOverlaySourceIsActive = true;
		}
		else
		{
			bdvPointOverlay.addPoint( doubles );
		}
	}

	private static AffineTransform3D asView( double[] doubles )
	{
		final AffineTransform3D view = new AffineTransform3D( );
		view.set( doubles );
		return view;
	}

	private static double[] asPosition4D( double[] doubles )
	{
		final double[] position4D = new double[ 4 ];
		for ( int d = 0; d < 3; d++ )
			position4D[ d ] = doubles[ d ];
		return position4D;
	}

	private static double[] getDoubles( String view )
	{
		if ( view.contains( "ViewerTransform" ) )
		{
			view = view.replace( "ViewerTransform: (", "" );
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
				view = view.replace( "(", "" );
				view = view.replace( ")", "" );
				return Utils.delimitedStringToDoubleArray( view, "," );
			} catch ( Exception e )
			{
				Utils.log( "Cannot parse view string :-(" );
				return null;
			}
		}
	}

	public static void enablePointOverlay( boolean isPointOverlayEnabled )
	{
		BdvViewChanger.isPointOverlayEnabled = isPointOverlayEnabled;
	}
}
