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

@CommandLine.Command(name = "mobie-files", mixinStandardHelpOptions = true, version = "3.0.13", description = "Visualise multi-modal big image data files, see https://mobie.github.io/")
public class FilesCmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = {"-r", "--root"}, required = false, description = "root folder that will be prepended to all other arguments")
	public String root;

	@Option(names = {"-i", "--image"}, required = false, description = "open an intensity image from a path, e.g., -i \"/home/image.tif\"; use wild-cards to open several images, e.g., -i \"/home/*-image.tif\"; specify several images by repeating the -i flag.")
	public String[] imageArray;

	@Option(names = {"-l", "--labels"}, required = false, description = "open a segmentation label mask image from a path, e.g. -s \"/home/labels.tif\"; same options as for --image")
	public String[] labelsArray;

	@Option(names = {"-t", "--table"}, required = false, description = "open a segment feature table matching the label image")
	public String[] tableArray;

	@Option(names = {"-g", "--grid"}, required = false, description = "grid type: none, stitched (default), transform")
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

		List< String > tables = tableArray == null ?
				Arrays.asList( tableArray ) : new ArrayList<>();

		if ( tables.size() > 1 )
		{
			System.out.println("Sorry, opening label tables is not yet implemented....");
		}

		new MoBIE( images, labels, root, gridType, settings );

		return null;
	}

	public static final void main( final String... args ) {

		final FilesCmd moBIECmd = new FilesCmd();

		if ( args == null || args.length == 0 )
			new CommandLine( moBIECmd ).execute( "--help" );
		else
			new CommandLine( moBIECmd ).setCaseInsensitiveEnumValuesAllowed( true ).execute( args );
	}
}