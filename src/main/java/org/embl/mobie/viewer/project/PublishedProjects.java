package org.embl.mobie.viewer.project;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class PublishedProjects
{
	private final HashMap< String, PublishedProject > publishedProjects = new LinkedHashMap<>();

	public PublishedProjects()
	{
		platy();
		covidIF();
		covidTomo();
		covidFIB();
		dAPEX();
		sponge();
	}

	private void sponge()
	{
		final PublishedProject project = new PublishedProject();
		project.name = "Musser et al. (2021) Sponge FIB-SEM, Science";
		project.location = "https://github.com/mobie/sponge-fibsem-project";
		project.publicationURL = "https://www.science.org/doi/abs/10.1126/science.abj2949";
		publishedProjects.put( project.name, project );
	}

	private void dAPEX()
	{
		final PublishedProject project = new PublishedProject();
		project.name = "Ayuso-Jimeno et al. (2022) PAG-dAPEX2 FIB-SEM, bioRxiv";
		project.location = "https://github.com/mobie/pag-dapex2-fibsem";
		project.publicationURL = "https://www.biorxiv.org/content/10.1101/2021.12.13.472405v1";
		publishedProjects.put( project.name, project );
	}

	private void covidFIB()
	{
		final PublishedProject project = new PublishedProject();
		project.name = "Cortese et al. (2020) Covid FIB-SEM, Cell Host & Microbe";
		project.location = "https://github.com/mobie/covid-em-project";
		project.publicationURL = "https://onlinelibrary.wiley.com/doi/full/10.1002/bies.202000257";
		publishedProjects.put( project.name, project );
	}

	private void covidTomo()
	{
		final PublishedProject project = new PublishedProject();
		project.name = "Cortese et al. (2020) Covid tomograms, Cell Host & Microbe";
		project.location = "https://github.com/mobie/covid-tomo-datasets";
		project.publicationURL = "https://www.sciencedirect.com/science/article/pii/S193131282030620X";
		publishedProjects.put( project.name, project );
	}

	private void platy()
	{
		final PublishedProject project = new PublishedProject();
		project.name = "Vergara et al. (2021) PlatyBrowser, Cell";
		project.location = "https://github.com/mobie/platybrowser-datasets";
		project.publicationURL = "https://www.biorxiv.org/content/10.1101/2020.02.26.961037v1";
		publishedProjects.put( project.name, project );
	}

	private void covidIF()
	{
		final PublishedProject project = new PublishedProject();
		project.name = "Pape et al. (2020) Covid Immunofluorescence, BioEssays";
		project.location = "https://github.com/mobie/covid-if-project";
		project.publicationURL = "https://onlinelibrary.wiley.com/doi/full/10.1002/bies.202000257";
		publishedProjects.put( project.name, project );
	}

	public HashMap< String, PublishedProject > getPublishedProjects()
	{
		return publishedProjects;
	}
}
