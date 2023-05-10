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
	// Measurements that are required for MoBIE: center
	run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding shape integrated skewness area_fraction redirect=None decimal=2");
	run("Analyze Particles...", "  show=[Count Masks] display clear");
}
