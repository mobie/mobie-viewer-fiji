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
package mobie3.viewer.project;

import java.util.ArrayList;
import java.util.List;

public class PublishedFigures
{
	public static final String MOBIE = "MoBIE";
	public static final String PLATY = "Platy";

	private final List< PublishedFigure > publishedFigures = new ArrayList<>();

	public PublishedFigures()
	{
		addMobieFigure( "Figure 1c (Platy-Atlas)", "https://github.com/mobie/platybrowser-project", "Figure1c" );
		addMobieFigure( "Figure 2a (CLEM)", "https://github.com/mobie/clem-example-project", "Figure2a" );
		addMobieFigure( "Figure 2b (CLEM)", "https://github.com/mobie/clem-example-project", "Figure2b" );
		addMobieFigure( "Figure 2c (CLEM)", "https://github.com/mobie/clem-example-project", "Figure2c" );
		addMobieFigure( "Figure 2d (CLEM)", "https://github.com/mobie/clem-example-project", "Figure2d" );
		addMobieFigure( "Figure 2e (CLEM)", "https://github.com/mobie/clem-example-project", "Figure2e" );
		addMobieFigure( "Figure 3a (HTM, long loading time)", "https://github.com/mobie/covid-if-project", "Figure3a" );
		addMobieFigure( "Figure 3b (HTM, long loading time)", "https://github.com/mobie/covid-if-project", "Figure3b" );
		addMobieFigure( "Figure 3c (HTM, long loading time)", "https://github.com/mobie/covid-if-project", "Figure3c" );
		addMobieFigure( "Figure 3d (HTM, long loading time)", "https://github.com/mobie/covid-if-project", "Figure3d" );
	}

	private void addMobieFigure( final String name, String project, String view )
	{
		final PublishedFigure figure = new PublishedFigure();
		figure.publicationAbbreviation = MOBIE;
		figure.name = name;
		figure.location = project;
		figure.view = view;
		figure.publicationURL = "TBD";
		publishedFigures.add( figure );
	}

	public List< PublishedFigure > getPublishedFigures()
	{
		return publishedFigures;
	}
}
