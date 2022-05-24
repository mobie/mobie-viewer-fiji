package org.embl.mobie.viewer.project;

import java.util.HashMap;

public class PublishedFigures
{
	private final HashMap< String, PublishedFigure > publishedFigures;

	public PublishedFigures()
	{
		publishedFigures = new HashMap<>();
		natureMethodsFigure2a();
	}

	private void natureMethodsFigure2a()
	{
		final PublishedFigure publishedFigure = new PublishedFigure();
		publishedFigure.name = "Nature Methods: Figure 2a";
		publishedFigure.location = "https://github.com/mobie/clem-example-project/";
		publishedFigure.view = "Figure2a";
		publishedFigure.publicationURL = "TBD";
		publishedFigures.put( publishedFigure.name, publishedFigure );
	}

	public HashMap< String, PublishedFigure > getPublishedFigures()
	{
		return publishedFigures;
	}
}
