package de.embl.cba.mobie.projects;

import java.util.HashMap;

public class PublishedProjectsCreator
{
	private final HashMap< String, PublishedProject > publishedProjects;

	public PublishedProjectsCreator()
	{
		publishedProjects = new HashMap<>();

		final PublishedProject project = new PublishedProject();
		project.name = "Vergara et al. (2020) \"PlatyBrowser\", bioRxiv";
		project.location = "https://github.com/platybrowser/platybrowser";
		project.pulicationURL = "https://www.biorxiv.org/content/10.1101/2020.02.26.961037v1";

		publishedProjects.put( project.name, project );
	}

	public HashMap< String, PublishedProject > getPublishedProjects()
	{
		return publishedProjects;
	}
}
