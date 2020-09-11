package de.embl.cba.mobie.bookmark;

import de.embl.cba.mobie.utils.Utils;

public class Location
{
	public LocationType type;
	public double[] doubles;

	public Location( LocationType type, double[] doubles )
	{
		this.type = type;
		this.doubles = doubles;
	}

	public Location( String location )
	{
		boolean isNormalised = false;

		if ( location.contains( "ViewerTransform" ) )
		{
			location = location.replace( "ViewerTransform: (", "" );
			location = location.replace( ")", "" );
		}

		if ( location.contains( "Position" ) )
		{
			location = location.replace( "Position: (", "" );
			location = location.replace( ")", "" );
		}

		if ( location.contains( "(" ))
			location = location.replace( "(", "" );

		if ( location.contains( ")" ))
			location = location.replace( ")", "" );

		if ( location.contains( "n" ) )
		{
			location = location.replace( ")", "" );
			isNormalised = true;
		}

		this.doubles = Utils.delimitedStringToDoubleArray( location, "," );

		if ( this.doubles.length == 3 )
			this.type = LocationType.Position3d;
		else if ( this.doubles.length == 4 )
			this.type = LocationType.Position3d;
		else if ( this.doubles.length == 12 )
		{
			if ( isNormalised )
				this.type = LocationType.NormalisedViewerTransform;
			else
				this.type = LocationType.ViewerTransform;
		}
	}
}
