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
		}
	};

	static List< ImageDataFormat > local = new ArrayList< ImageDataFormat >() {
		{
			// local
			add( ImageDataFormat.SpimData );
			add( ImageDataFormat.OmeZarr );
			add( ImageDataFormat.BdvOmeZarr );
			add( ImageDataFormat.BdvN5 );
			add( ImageDataFormat.BdvHDF5 );
			add( ImageDataFormat.Toml );
			add( ImageDataFormat.Tiff );
			add( ImageDataFormat.ImageJ );
			add( ImageDataFormat.BioFormats );
			add( ImageDataFormat.Bdv );
			add( ImageDataFormat.IlastikHDF5 );
		}
	};


	static List< ImageDataFormat > remoteLocal = new ArrayList< ImageDataFormat >() {
		{
			addAll( remote );
			addAll( local );
		}
	};

	static List< ImageDataFormat > localRemote = new ArrayList< ImageDataFormat >() {
		{
			addAll( local );
			addAll( remote );
		}
	};


	public static List< ImageDataFormat > getImageDataFormats( Location preferential )
	{
		if ( preferential.equals( Location.Remote ) )
		{
			return remoteLocal;
		}
		else // Location.Local
		{
			return localRemote;
		}
	}
}
