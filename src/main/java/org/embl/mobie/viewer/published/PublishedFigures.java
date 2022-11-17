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
package org.embl.mobie.viewer.published;

import java.util.ArrayList;
import java.util.List;

public class PublishedFigures
{
	public static final String MOBIE = "MoBIE";
	public static final String PLATY = "Platy";

	private final List< PublishedFigure > publishedFigures = new ArrayList<>();

	public PublishedFigures()
	{
        // views for panels in Figure 1
		addMobieFigure( "Figure 1a (Platy-Atlas)", "https://github.com/mobie/platybrowser-project", "Figure1a" );
		addMobieFigure( "Figure 1c (CLEM)", "https://github.com/mobie/clem-example-project", "SupplFig1b" );
		addMobieFigure( "Figure 1c (HTM)", "https://github.com/mobie/covid-if-project", "SupplFig1a" );
		addMobieFigure( "Figure 1c (Timeseries)", "https://github.com/mobie/arabidopsis-root-lm-project", "Figure1c" );
		addMobieFigure( "Figure 1c (Spatial Transcriptomics, long loading time)", "https://github.com/mobie/mouse-embryo-spatial-transcriptomics-project", "SupplFig3a" );

		addMobieFigure( "Suppl. Figure 1a (CLEM)", "https://github.com/mobie/clem-example-project", "SupplFig1a" );
		addMobieFigure( "Suppl. Figure 1b (CLEM)", "https://github.com/mobie/clem-example-project", "SupplFig1b" );
		addMobieFigure( "Suppl. Figure 1c (CLEM)", "https://github.com/mobie/clem-example-project", "SupplFig1c" );
		addMobieFigure( "Suppl. Figure 1d (CLEM)", "https://github.com/mobie/clem-example-project", "SupplFig1d" );
		addMobieFigure( "Suppl. Figure 1e (CLEM)", "https://github.com/mobie/clem-example-project", "SupplFig1e" );

		addMobieFigure( "Suppl. Figure 2a (HTM)", "https://github.com/mobie/clem-example-project", "SupplFig2a" );
		addMobieFigure( "Suppl. Figure 2b (HTM)", "https://github.com/mobie/clem-example-project", "SupplFig2b" );
		addMobieFigure( "Suppl. Figure 2c (HTM)", "https://github.com/mobie/clem-example-project", "SupplFig2c" );
		addMobieFigure( "Suppl. Figure 2d (HTM)", "https://github.com/mobie/clem-example-project", "SupplFig2d" );
		
        addMobieFigure( "Suppl. Figure 3a (Spatial Transcriptomics, long loading time)", "https://github.com/mobie/mouse-embryo-spatial-transcriptomics-project", "SupplFig3a" );
		addMobieFigure( "Suppl. Figure 3b (Spatial Transcriptomics, long loading time)", "https://github.com/mobie/mouse-embryo-spatial-transcriptomics-project", "SupplFig3b" );
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
