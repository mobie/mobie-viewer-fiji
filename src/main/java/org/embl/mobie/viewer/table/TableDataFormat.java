package org.embl.mobie.viewer.table;

import com.google.gson.annotations.SerializedName;

public enum TableDataFormat
{
	@SerializedName( "tsv" )
	TabDelimitedFile;

	@Override
	public String toString()
	{
		switch ( this )
		{
			case TabDelimitedFile:
				return "tsv";
			default:
				throw new UnsupportedOperationException( "Unknown file format: " + this );
		}
	}
}
