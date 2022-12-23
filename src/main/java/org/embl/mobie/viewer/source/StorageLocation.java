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
package org.embl.mobie.viewer.source;

public class StorageLocation
{
	// for data on disk in a MoBIE project
	public String relativePath;

	// for other data on disk
	public String absolutePath;

	// for data on s3
	public String s3Address;
	public String signingRegion;

	// MoBIE is for big data,
	// thus the above locations typically refer to
	// a container, which may contain multiple
	// chunks of data that are lazy loaded.
	// for images, those chunks are specified
	// by the file format.
	// for tables, we don't yet have such
	// a specification; to solve this
	// the {@code defaultChunk} points to the chunk
	// (typically a file in the {@code absolutePath} folder),
	// that contains the default columns.
	// additional available chunks may be discovered,
	// e.g. by looking into {@code absolutePath}.
	public String defaultChunk;

	// for data in RAM
	public Object data;

	// the image data model in MoBIE is single channel,
	// and thus one needs to subset a potentially
	// multi-channel image data source.
	//
	// as data is opened as {@code SpimData}
	// the channel can also refer to a setup, which
	// does not need to be a channel, but could be some
	// entirely other image, e.g. in CZI or LIF files
	//
	// historically, we only had channels, and thus the name
	// of the variable is "channel" and cannot readily be changed
	// because we use in the MoBIE JSON spec
	public Integer channel = 0;

}
