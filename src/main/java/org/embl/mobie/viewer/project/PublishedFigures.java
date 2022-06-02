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

import java.util.ArrayList;
import java.util.List;

public class PublishedFigures
{
	public static final String MOBIE = "MoBIE";
	public static final String PLATY = "Platy";

	private final List< PublishedFigure > publishedFigures = new ArrayList<>();

	public PublishedFigures()
	{
		mobieFigure1c();
		mobieFigure2( "a" );
		mobieFigure2( "b" );
		mobieFigure2( "c" );
		mobieFigure2( "d" );
		mobieFigure2( "e" );
		mobieFigure3( "a" );
		mobieFigure3( "b" );
		mobieFigure3( "c" );
		mobieFigure3( "d" );
	}

	private void mobieFigure1c()
	{
		final PublishedFigure figure = new PublishedFigure();
		figure.publicationAbbreviation = MOBIE;
		figure.name = "Figure 1c";
		figure.location = "https://github.com/mobie/platybrowser-project";
		figure.view = "Figure1c";
		figure.publicationURL = "TBD";
		publishedFigures.add( figure );
	}

	private void mobieFigure2a()
	{
		final PublishedFigure figure = new PublishedFigure();
		figure.publicationAbbreviation = MOBIE;
		figure.name = "Figure 2" + panel;
		figure.location = "https://github.com/mobie/clem-example-project";
		figure.view = "Figure2" + panel;
		figure.publicationURL = "https://github.com/mobie/clem-example-project";
		publishedFigures.add( figure );
	}

	private void mobieFigure3( final String panel )
	{
		final PublishedFigure figure = new PublishedFigure();
		figure.publicationAbbreviation = MOBIE;
		figure.name = "Figure 3" + panel;
		figure.location = "https://github.com/mobie/covid-if-project";
		figure.view = "Figure3" + panel;
		figure.publicationURL = "TBD";
		publishedFigures.add( figure );
	}

	public List< PublishedFigure > getPublishedFigures()
	{
		return publishedFigures;
	}
}
