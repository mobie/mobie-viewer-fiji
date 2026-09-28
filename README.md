[![Build Status](https://github.com/mobie/mobie-viewer-fiji/actions/workflows/build.yml/badge.svg)](https://github.com/mobie/mobie-viewer-fiji/actions/workflows/build.yml)
[![Maven Scijava Version](https://img.shields.io/github/v/tag/mobie/mobie-viewer-fiji?label=Version-[Maven%20Scijava])](https://maven.scijava.org/#browse/browse:releases:org%2Fembl%2Fmobie%2Fmobie-viewer-fiji)


[![DOI](https://zenodo.org/badge/177135630.svg)](https://zenodo.org/badge/latestdoi/177135630)

# MoBIE Fiji Viewer

MoBIE is a Fiji plugin for exploring and sharing big multi-modal image and associated tabular data.

## History

The MoBIE was initially developed to explore a cellular atlas for the biological model system _Platynereis dumerilii_, see: [Whole-body integration of gene expression and single-cell morphology](https://www.sciencedirect.com/science/article/pii/S009286742100876X). However, the framework turned out to be so useful and generalisable that it is now used for other projects as well, e.g. see all the repositories ending on "-project" [here](https://github.com/mobie).

## Updates

Please check the [MoBIE Newsfeed](https://forum.image.sc/t/mobie-updates-newsfeed/111262)

## Cite

If you use MoBIE, please cite:

[Pape, C., Meechan, K., Moreva, E. et al. MoBIE: a Fiji plugin for sharing and exploration of multi-modal cloud-hosted big image data. Nat Methods (2023). https://doi.org/10.1038/s41592-023-01776-4](https://www.nature.com/articles/s41592-023-01776-4).

## Documentation & Tutorials

Detailed tutorials for installing & using MoBIE are available at [https://mobie.github.io/](https://mobie.github.io/).

## Installation

Currently if you [install Fiji](https://fiji.sc) there are two options: Latest or Stable. 
This choice affects how you need to install MoBIE.

### Fiji-latest (recommend)

1. Please [install Fiji Latest](https://fiji.sc) on your computer.
2. Start Fiji and install the MoBIE-latest update site:
	- `Help > Update`
	- `[ Manage Update Sites ]`
	- `[ Add Unlisted Site ]`
		- Name: `MoBIE-latest`
		- URL: `https://sites.imagej.net/MoBIE-
    - `[X] OME-Zarr` (optional, provides additional OME-Zarr reader backends within MoBIE)
    - `[X] BigVolumeBrowser` (optional, enables BigVolumeBrowser visualisation within MoBIE)
3. Restart Fiji

### Fiji-stable

1. Please [install Fiji Stable](https://fiji.sc) on your computer.
2. Restart Fiji and install the MoBIE update site ([how to install an update site](https://imagej.net/Following_an_update_site#Introduction)).
    - [X] `MoBIE`
3. Restart Fiji

## Quick start

### Open a MoBIE project

1. In the Fiji search bar, type: "mobie"<br> <img width="460" alt="image" src="https://user-images.githubusercontent.com/2157566/86445323-79dfea00-bd12-11ea-8884-5e50a08660d0.png"> <br> ...and click [ Run ]
2. Enter a github repository (e.g., `https://github.com/mobie/platybrowser-project`) representing your datasets <br><img width="300" alt="image" src="https://user-images.githubusercontent.com/2157566/86445504-cdeace80-bd12-11ea-996a-4a6d5d58ccc7.png">
3. The MoBIE viewer is ready to be used:<br><img width="800" alt="image" src="https://user-images.githubusercontent.com/2157566/86445771-42be0880-bd13-11ea-9627-cd1ee7b62a99.png">
