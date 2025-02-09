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

import org.embl.mobie.lib.util.MoBIEHelper;

import java.io.File;

public class FileImageSource
{
	public String name;
	public String path;
	public Integer channelIndex = 0;

	/**
	 * Parses the input string assuming the pattern:
	 * "path=name;channelIndex"
	 * where everything after path is optional
	 *
	 * @param uri
	 * 				the uri to be parsed
	 */
	public FileImageSource( String uri )
	{
		String[] split = new String[]{ uri };
		if ( uri.contains( ";" ) )
		{
			split = uri.split( ";" );
			channelIndex = Integer.parseInt( split[ 1 ] );
		}

		if ( split[ 0 ].contains( "=" ) )
		{
			split = split[ 0 ].split( "=" );
			path = split[ 0 ];
			name = split[ 1 ];
		}
		else
		{
			name = MoBIEHelper.removeExtension( new File( split[ 0 ] ).getName() );
			path = split[ 0 ];
		}
	}
}
