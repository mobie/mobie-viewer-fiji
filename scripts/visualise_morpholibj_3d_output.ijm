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
threshold = 107;

// Code
//
run("Close All");
run("Options...", "iterations=1 count=1 black do=Nothing");
analyseNuclei3D( "Nuclei3D", "/Users/tischer/Documents/training-resources/image_data/xyz_8bit__nuclei_autothresh.tif", threshold );
run("View Image and Segmentation and Table...","image=INCENP_T1 labels=INCENP_T1_binary-lbl table=INCENP_T1_binary-lbl-Morphometry");


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
