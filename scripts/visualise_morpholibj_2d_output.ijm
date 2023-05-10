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
threshold = 25;

// Code
//
run("Close All");
run("Options...", "iterations=1 count=1 black do=Nothing");
analyseNuclei( "INCENP_T1", "/Users/tischer/Documents/training-resources/image_data/xy_8bit__mitocheck_incenp_t1.tif", threshold );

//analyseNuclei( "INCENP_T1", "https://github.com/NEUBIAS/training-resources/raw/master/image_data/xy_8bit__mitocheck_incenp_t1.tif", threshold );
//analyseNuclei( "INCENP_T70", "https://github.com/NEUBIAS/training-resources/raw/master/image_data/xy_8bit__mitocheck_incenp_t70.tif", threshold );
//run("Tile");

// Functions
//
function analyseNuclei( name, filePath, threshold )
{
	open(filePath);
	rename(name);
	setMinAndMax(0, 100);
	run("Duplicate...", "title=" + name + "_binary" );
	setThreshold(threshold, 65535);
	run("Convert to Mask");
	run("Connected Components Labeling", "connectivity=4 type=[8 bits]");
	// Measurements that are required for MoBIE: centroid
	run("Analyze Regions", "area perimeter circularity euler_number bounding_box centroid equivalent_ellipse ellipse_elong. convexity max._feret oriented_box oriented_box_elong. geodesic tortuosity max._inscribed_disc average_thickness geodesic_elong.");
}
