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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class TimepointsTransformation< T > extends AbstractImageTransformation< T, T >
{
	// Serialisation

	/**
	 * Outer list: list of timepoints that you want to map
	 * Inner list: pairs of timepoints: to be read as
	 * mapping the timepoint of the new source to the old source, i.e.
	 * new -> old
	 * "old" must exist in the input source
	 * "new" will exist in the transformed source
	 * "new" timepoints must be unique
	 * "old" may be referred to several times
	 */
	protected List< List< Integer > > parameters;

	/**
	 * Whether to keep the timepoints that are present in the source
	 */
	protected boolean keep = false;

	// For GSON
	public TimepointsTransformation()
	{
	}

	public TimepointsTransformation( String name, List< List< Integer > > timepoints, boolean keep, List< String > sources ) {
		this( name, timepoints, keep, sources, null );
	}

	public TimepointsTransformation( String name, List< List< Integer > > timepoints, boolean keep, List< String > sources, List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.parameters = timepoints;
		this.keep = keep;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
	}

	public HashMap< Integer, Integer > getTimepointsMapping()
	{
		final HashMap< Integer, Integer > timepointMap = new LinkedHashMap<>();
		for ( List< Integer > newOld : parameters )
		{
			timepointMap.put( newOld.get( 0 ), newOld.get( 1 ) );
		}

		return timepointMap;
	}

	public boolean isKeep()
	{
		return keep;
	}

	@Override
	public List< String > getSources()
	{
		return sources;
	}
}
