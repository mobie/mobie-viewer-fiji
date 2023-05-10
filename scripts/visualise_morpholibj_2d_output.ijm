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
root = "/Users/tischer/Documents/training-resources/image_data/"
//root = "https://github.com/NEUBIAS/training-resources/raw/master/image_data/"

// Code
//
run("Close All");
run("Options...", "iterations=1 count=1 black do=Nothing");
analyseNuclei( "INCENP_T1", root+"xy_8bit__mitocheck_incenp_t1.tif", threshold );
run("View Image and Labels and Table...", "image=INCENP_T1 labels=INCENP_T1_binary-lbl table=INCENP_T1_binary-lbl-Morphometry");

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
