package de.embl.cba.mobie.source;

import com.google.gson.annotations.SerializedName;

public enum ImageDataFormat
{
	@SerializedName( "bdv.n5" )
	BdvN5,
	@SerializedName( "bdv.n5.s3" )
	BdvN5S3,
	@SerializedName( "openOrganelle" )
	OpenOrganelle;

	@Override
	public String toString()
	{
		switch ( this )
		{
			case BdvN5:
				return "bdv.n5";
			case BdvN5S3:
				return "bdv.n5.s3";
			case OpenOrganelle:
				return "openOrganelle";
			default:
				throw new UnsupportedOperationException( "Unknown image file format: " + this );
		}
	}
}
