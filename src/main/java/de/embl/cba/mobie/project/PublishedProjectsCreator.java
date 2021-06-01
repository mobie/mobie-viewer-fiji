package de.embl.cba.mobie.project;

import java.util.HashMap;

public class PublishedProjectsCreator
{
	private final HashMap< String, PublishedProject > publishedProjects;

	public PublishedProjectsCreator()
	{
		publishedProjects = new HashMap<>();

		final PublishedProject platybrowserProject = new PublishedProject();
		platybrowserProject.name = "Vergara et al. (2020) \"PlatyBrowser\", bioRxiv";
		platybrowserProject.location = "https://github.com/mobie/platybrowser-datasets";
		platybrowserProject.pulicationURL = "https://www.biorxiv.org/content/10.1101/2020.02.26.961037v1";

		publishedProjects.put( platybrowserProject.name, platybrowserProject );

		final PublishedProject covidTomoProject = new PublishedProject();
		covidTomoProject.name = "Cortese et al. (2020) covid tomograms, Cell Host & Microbe";
		covidTomoProject.location = "https://github.com/mobie/covid-tomo-datasets";
		covidTomoProject.pulicationURL = "https://www.sciencedirect.com/science/article/pii/S193131282030620X";

		publishedProjects.put( covidTomoProject.name, covidTomoProject );

		final PublishedProject covidFIBProject = new PublishedProject();
		covidFIBProject.name = "Cortese et al. (2020) covid FIB-SEM, Cell Host & Microbe";
		covidFIBProject.location = "https://github.com/mobie/covid-em-datasets";
		covidFIBProject.pulicationURL = "https://www.sciencedirect.com/science/article/pii/S193131282030620X";

		publishedProjects.put( covidFIBProject.name, covidFIBProject );

	}

	public HashMap< String, PublishedProject > getPublishedProjects()
	{
		return publishedProjects;
	}
}
