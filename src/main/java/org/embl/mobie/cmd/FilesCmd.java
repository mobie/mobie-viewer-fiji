package org.embl.mobie.cmd;

import net.imagej.ImageJ;
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

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image path, e.g., -i \"/home/image.tif\"; use wild-cards to open several images, e.g., -i \"/home/*-image.tif\"; specify several images by repeating the -i flag.")
	public String[] images;

	@Option(names = {"-l", "--labels"}, required = false, description = "label mask image path, e.g. -s \"/home/labels.tif\"; same options as for --image")
	public String[] labels;

	@Option(names = {"-t", "--table"}, required = false, description = "segment feature table path, matching the label image")
	public String[] tables;

	@Option(names = {"-g", "--grid"}, required = false, description = "grid type: none, stitched (default), transform")
	public GridType gridType = GridType.Stitched;

	@Option(names = {"--remove-spatial-calibration"}, required = false, description = "removes spatial calibration from all images; this is useful if only some images have a spatial calibration and thus the overlay would fail.")
	public Boolean removeSpatialCalibration = false;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings()
				.cli( true )
				.removeSpatialCalibration( removeSpatialCalibration );

		List< String > imageList = images != null ?
				Arrays.asList( images ) : new ArrayList<>();

		List< String > labelsList = labels != null ?
				Arrays.asList( labels ) : new ArrayList<>();

		List< String > tablesList = tables != null ?
				Arrays.asList( tables ) : new ArrayList<>();

		if ( tablesList.size() > 1 )
		{
			System.out.println("Sorry, opening label tables is not yet implemented....");
		}

		new ImageJ().ui().showUI();
		new MoBIE( imageList, labelsList, root, gridType, settings );

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