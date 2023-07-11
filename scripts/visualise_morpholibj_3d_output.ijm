/**
 * ImageJ Macro for 2D Nuclei segmentation and visualisation in MoBIE
 * 
 * Adapted from https://neubias.github.io/training-resources/all-modules/
 * 
 * Required update sites:
 *   - IJPB-Plugins (MorpholibJ)
 *   - MoBIE
 * 
 */

// Parameters
//
threshold = 80;
root = "/Users/tischer/Documents/training-resources/image_data/"
//root = "https://github.com/NEUBIAS/training-resources/raw/master/image_data/"

// Code
//
run("Close All");
run("Options...", "iterations=1 count=1 black do=Nothing");
analyseNuclei3D( "Nuclei3D", root + "xyz_8bit__nuclei_autothresh.tif", threshold );

// View in MoBIE
run("View Image and Labels and Table...", "image=Nuclei3D labels=Nuclei3D_binary-lbl table=Nuclei3D_binary-lbl-morpho");


// Functions
//
function analyseNuclei3D( name, filePath, threshold )
{
	open(filePath);
	rename(name);
	run("Duplicate...", "title=" + name + "_binary duplicate" );
	setThreshold(threshold, 65535);
	run("Convert to Mask", "method=Default background=Default black");
	run("Connected Components Labeling", "connectivity=6 type=[16 bits]");
	run("Analyze Regions 3D", "volume surface_area mean_breadth sphericity euler_number bounding_box centroid equivalent_ellipsoid ellipsoid_elongations max._inscribed surface_area_method=[Crofton (13 dirs.)] euler_connectivity=C26");
}
