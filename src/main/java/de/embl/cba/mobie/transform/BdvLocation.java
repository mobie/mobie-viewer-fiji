package de.embl.cba.mobie.transform;

import de.embl.cba.mobie.Utils;

public class BdvLocation
{
	public BdvLocationType type;
	public double[] doubles;

	public BdvLocation( BdvLocationType type, double[] doubles )
	{
		this.type = type;
		this.doubles = doubles;
	}

	public BdvLocation( String location )
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
			location = location.replace( "n", "" );
			isNormalised = true;
		}

		this.doubles = Utils.delimitedStringToDoubleArray( location, "," );

		if ( this.doubles.length == 3 )
			this.type = BdvLocationType.Position3d;
		else if ( this.doubles.length == 4 )
			this.type = BdvLocationType.Position3d;
		else if ( this.doubles.length == 12 )
		{
			if ( isNormalised )
				this.type = BdvLocationType.NormalisedViewerTransform;
			else
				this.type = BdvLocationType.ViewerTransform;
		}
	}
}
