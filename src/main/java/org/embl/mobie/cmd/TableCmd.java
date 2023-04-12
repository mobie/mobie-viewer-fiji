package org.embl.mobie.cmd;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.lib.transform.GridType;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie-table", mixinStandardHelpOptions = true, version = "3.0.13", description = "Visualise images and labels masks from a dataset table, see https://mobie.github.io/")
public class TableCmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = {"-t", "--table"}, required = true, description = "dataset table, each row corresponding to one image or to one object in an image")
	public String table;

	@Option(names = {"-r", "--root"}, required = false, description = "root folder that will be prepended to all image and label column entries")
	public String root;

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image column; to open multiple columns repeat the -i parameter; to replace the column name use, e.g., \"Nuclei=FilePath_DAPI\"; to only open one channel from a multi-channel file use, e.g., \"Nuclei=FilePath_Image;0\"")
	public String[] imageArray;

	@Option(names = {"-l", "--labels"}, required = false, description = "label mask image column; see --images for further explanations")
	public String[] labelsArray;

	@Option(names = {"-g", "--grid"}, required = false, description = "grid type: none, stitched (default), transform; \"stitched\" should yield the best performance but requires that all images have the same dimensions")
	public GridType gridType = GridType.Stitched;

	@Option(names = {"--remove-spatial-calibration"}, required = false, description = "removes spatial calibration from all images; this is useful if only some images have a spatial calibration and thus the overlay would fail.")
	public Boolean removeSpatialCalibration = false;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings()
				.cli( true )
				.removeSpatialCalibration( removeSpatialCalibration );

		List< String > images = imageArray == null ?
				Arrays.asList( imageArray ) : new ArrayList<>();

		List< String > labels = labelsArray == null ?
				Arrays.asList( labelsArray ) : new ArrayList<>();

		new MoBIE( table, images, labels, root, gridType, settings );

		return null;
	}

	public static final void main( final String... args ) {

		final TableCmd moBIECmd = new TableCmd();

		if ( args == null || args.length == 0 )
			new CommandLine( moBIECmd ).execute( "--help" );
		else
			new CommandLine( moBIECmd ).setCaseInsensitiveEnumValuesAllowed( true ).execute( args );
	}
}