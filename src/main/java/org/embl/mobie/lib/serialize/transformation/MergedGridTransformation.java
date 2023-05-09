/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.serialize.transformation;

import java.util.List;

public class MergedGridTransformation extends AbstractGridTransformation
{
	// Serialization TODO: make fields private and add getter and setter methods

	public List< String > sources; // required

	private String mergedGridSourceName = "merged image"; // required => name of corresponding StitchedImage

	public String metadataSource; // optional, this improves performance (a lot)

	public boolean centerAtOrigin = false; // TODO: should actually be true, but: https://github.com/mobie/mobie-viewer-fiji/issues/685#issuecomment-1108179599

	// Runtime
	public transient int timepoints = 1; // the number of timepoints that the sources span

	public transient boolean lazyLoadTables = true;

	// Needed for GSON to populate the default values
	public MergedGridTransformation()
	{
		super();
	}

	public MergedGridTransformation( String mergedGridSourceName )
	{
		this.mergedGridSourceName = mergedGridSourceName;
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}

	public String getName()
	{
		return mergedGridSourceName;
	}

}
