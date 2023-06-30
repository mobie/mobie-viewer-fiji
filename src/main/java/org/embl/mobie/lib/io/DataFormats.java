package org.embl.mobie.lib.io;

import org.embl.mobie.io.ImageDataFormat;

import java.util.ArrayList;
import java.util.List;

public class DataFormats
{
	public enum	Location
	{
		Remote,
		Local;
	}

	static List< ImageDataFormat > remote = new ArrayList< ImageDataFormat >() {
		{
			// remote
			add( ImageDataFormat.OmeZarrS3 );
			add( ImageDataFormat.BdvOmeZarrS3 );
			add( ImageDataFormat.BdvN5S3 );
			add( ImageDataFormat.OpenOrganelleS3 );

			// local
			add( ImageDataFormat.OmeZarr );
			add( ImageDataFormat.BdvOmeZarr );
			add( ImageDataFormat.BdvN5 );
			add( ImageDataFormat.BdvHDF5 );
			add( ImageDataFormat.ImageJ );
			add( ImageDataFormat.BioFormats );
		}
	};

	static List< ImageDataFormat > local = new ArrayList< ImageDataFormat >() {
		{
			// local
			add( ImageDataFormat.OmeZarr );
			add( ImageDataFormat.BdvOmeZarr );
			add( ImageDataFormat.BdvN5 );
			add( ImageDataFormat.BdvHDF5 );
			add( ImageDataFormat.ImageJ );
			add( ImageDataFormat.BioFormats );

			// remote
			add( ImageDataFormat.OmeZarrS3 );
			add( ImageDataFormat.BdvOmeZarrS3 );
			add( ImageDataFormat.BdvN5S3 );
			add( ImageDataFormat.OpenOrganelleS3 );
		}
	};


	public static List< ImageDataFormat > getImageDataFormats( Location preferential )
	{
		if ( preferential.equals( Location.Remote ) )
		{
			return remote;
		}
		else // Location.Local
		{
			return local;
		}
	}
}
