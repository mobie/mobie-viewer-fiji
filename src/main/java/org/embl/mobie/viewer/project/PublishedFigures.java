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
package org.embl.mobie.viewer.project;

import org.scijava.java3d.Link;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class PublishedFigures
{
	private final HashMap< String, PublishedFigure > publishedFigures = new LinkedHashMap<>();

	public PublishedFigures()
	{
		bioRxivFigure1c();
		bioRxivFigure2a();
		bioRxivFigure3( "a" );
		bioRxivFigure3( "b" );
		bioRxivFigure3( "c" );
		bioRxivFigure3( "d" );
	}

	private void bioRxivFigure1c()
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "bioRxiv: Figure 1c";
		publishedFigure.location = "https://github.com/mobie/platybrowser-project";
		publishedFigure.view = "Figure1c";
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	private void bioRxivFigure2a()
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "bioRxiv: Figure 2a";
		publishedFigure.location = "https://github.com/mobie/clem-example-project";
		publishedFigure.view = "Figure2a";
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	private void bioRxivFigure3( final String panel )
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "bioRxiv: Figure 3" + panel;
		publishedFigure.location = "https://github.com/mobie/covid-if-project";
		publishedFigure.view = "Figure3"+panel;
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	public HashMap< String, PublishedFigure > getPublishedFigures()
	{
		return publishedFigures;
	}
}
