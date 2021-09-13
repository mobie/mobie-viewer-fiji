package de.embl.cba.mobie.source;

import com.google.gson.annotations.SerializedName;

public enum ImageDataFormat
{
	@SerializedName( "bdv.n5" )
	BdvN5,
	@SerializedName( "bdv.n5.s3" )
	BdvN5S3,
	@SerializedName( "openOrganelle.s3" )
	OpenOrganelleS3,
	@SerializedName("bdv.ome.zarr")
	BdvOmeZarr,
    @SerializedName("bdv.ome.zarr.s3")
    BdvOmeZarrS3,
	@SerializedName("ome.zarr")
	OmeZarr,
    @SerializedName("ome.zarr.s3")
    OmeZarrS3;

	// needed for SciJava Command UI, which does not support enums
	public static final String BDVN5 = "BdvN5";
	public static final String BDVN5S3 = "BdvN5S3";
	public static final String OPENORGANELLES3 = "OpenOrganelleS3";
	public static final String BDVOMEZARR = "BdvOmeZarr";
	public static final String BDVOMEZARRS3 = "BdvOmeZarrS3";
	public static final String OMEZARR = "OmeZarr";
	public static final String OMEZARRS3 = "OmeZarrS3";

	@Override
	public String toString()
	{
		switch ( this )
		{
			case BdvN5:
				return "bdv.n5";
			case BdvN5S3:
				return "bdv.n5.s3";
			case OpenOrganelleS3:
				return "openOrganelle.s3";
			case BdvOmeZarr:
				return "bdv.ome.zarr";
			case OmeZarr:
				return "ome.zarr";
            case BdvOmeZarrS3:
                return "bdv.ome.zarr.s3";
            case OmeZarrS3:
                return "ome.zarr.s3";
			default:
				throw new UnsupportedOperationException( "Unknown file format: " + this );
		}
	}

	public static ImageDataFormat fromString( String string )
	{
		switch ( string )
		{
			case "bdv.n5":
				return BdvN5;
			case "bdv.n5.s3":
				return BdvN5S3;
			case "openOrganelle":
				return OpenOrganelleS3;
			case "bdv.ome.zarr":
				return BdvOmeZarr;
			case "ome.zarr":
				return OmeZarr;
            case "bdv.ome.zarr.s3":
                return BdvOmeZarrS3;
            case "ome.zarr.s3":
                return OmeZarrS3;
			default:
				throw new UnsupportedOperationException( "Unknown file format: " + string );
		}
	}

	public boolean isRemote() {
		switch ( this )
		{
			case BdvN5S3:
			case OmeZarrS3:
			case BdvOmeZarrS3:
			case OpenOrganelleS3:
				return true;
			case BdvN5:
			case BdvOmeZarr:
			case OmeZarr:
			default:
				return false;
		}
	}

	public boolean hasXml() {
		switch ( this )
		{
			case BdvN5S3:
			case BdvOmeZarr:
			case BdvN5:
			case BdvOmeZarrS3:
				return true;
			case OmeZarr:
			case OpenOrganelleS3:
			case OmeZarrS3:
			default:
				return false;
		}

	}

}
