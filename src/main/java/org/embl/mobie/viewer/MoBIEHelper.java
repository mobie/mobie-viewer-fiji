/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer;

import ij.ImagePlus;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.io.File;


public abstract class MoBIEHelper
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static int[] asInts( long[] longs) {
		int[] ints = new int[longs.length];

		for(int i = 0; i < longs.length; ++i)
			ints[i] = (int)longs[i];

		return ints;
	}

	public static long[] asLongs( int[] ints) {
		long[] longs = new long[ints.length];

		for(int i = 0; i < longs.length; ++i)
			longs[i] = ints[i];

		return longs;
	}

	public static ImagePlus openWithBioFormats( String path, int seriesIndex )
	{
		try
		{
			ImporterOptions opts = new ImporterOptions();
			opts.setId( path );
			opts.setVirtual( true );
			opts.setSeriesOn( seriesIndex, true );
			ImportProcess process = new ImportProcess( opts );
			process.execute();
			ImagePlusReader impReader = new ImagePlusReader( process );
			ImagePlus[] imps = impReader.openImagePlus();
			return imps[ 0 ];
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public enum FileLocation
	{
		Project,
		FileSystem
	}

	// TODO: Proper implementation in IOHelper in mobie-io
	public static String getFileName( String path )
	{
		if ( path.startsWith( "http" ) )
		{
			final String[] split = path.split( "/" );
			return split[ split.length - 1 ];
		}
		else
		{
			return new File( path ).getName();
		}
	}
}
