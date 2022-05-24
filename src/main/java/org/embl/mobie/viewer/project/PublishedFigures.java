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
		publishedFigure.location = "https://github.com/mobie/coivd-if-project";
		publishedFigure.view = "Figure3"+panel;
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	public HashMap< String, PublishedFigure > getPublishedFigures()
	{
		return publishedFigures;
	}
}
