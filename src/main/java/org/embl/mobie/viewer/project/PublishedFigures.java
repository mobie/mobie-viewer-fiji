package org.embl.mobie.viewer.project;

import org.scijava.java3d.Link;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class PublishedFigures
{
	private final HashMap< String, PublishedFigure > publishedFigures = new LinkedHashMap<>();

	public PublishedFigures()
	{
		natureMethodsFigure1c();
		natureMethodsFigure2a();
		natureMethodsFigure3( "a" );
		natureMethodsFigure3( "b" );
		natureMethodsFigure3( "c" );
		natureMethodsFigure3( "d" );
	}

	private void natureMethodsFigure1c()
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "Nature Methods: Figure 1c";
		publishedFigure.location = "https://github.com/mobie/platybrowser-project";
		publishedFigure.view = "Figure1c";
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	private void natureMethodsFigure2a()
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "Nature Methods: Figure 2a";
		publishedFigure.location = "https://github.com/mobie/clem-example-project";
		publishedFigure.view = "Figure2a";
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	private void natureMethodsFigure3( final String panel )
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "Nature Methods: Figure 3" + panel;
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
