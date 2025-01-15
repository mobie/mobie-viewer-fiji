/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
			add( ImageDataFormat.OmeZarrS3 );
			add( ImageDataFormat.BdvOmeZarrS3 );
			add( ImageDataFormat.BdvN5S3 );
			add( ImageDataFormat.OpenOrganelleS3 );
			add( ImageDataFormat.BioFormatsS3 );
			// add( ImageDataFormat.N5 ); // FIXME: Why is this here?
		}
	};

	static List< ImageDataFormat > local = new ArrayList< ImageDataFormat >() {
		{
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
			add( ImageDataFormat.Ilastik );
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
