package org.embl.mobie.cmd;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.plugin.Parameter;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie-hcs", mixinStandardHelpOptions = true, version = "3.0.13", description = "Visualise high content screening image data, see https://mobie.github.io/tutorials/hcs.html")
public class HCSCmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = {"-h", "--hcs"}, required = true, description = "folder containing HCS image data")
	public String hcs;

	@Option(names = {"-w", "--well-margin"}, required = false, description = "relative well margin (default = 0.1)")
	public double wellMargin = 0.1;

	@Option(names = {"-s", "--site-margin"}, required = false, description = "relative site margin (default = 0.0)")
	public double siteMargin = 0.0;

	@Option(names = {"--remove-spatial-calibration"}, required = false, description = "removes spatial calibration from all images; this is useful if only some images have a spatial calibration and thus the overlay would fail.")
	public Boolean removeSpatialCalibration = false;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings()
				.cli( true )
				.removeSpatialCalibration( removeSpatialCalibration );

		new MoBIE( hcs, settings, wellMargin, siteMargin );

		return null;
	}

	public static final void main( final String... args ) {

		final HCSCmd moBIECmd = new HCSCmd();

		if ( args == null || args.length == 0 )
			new CommandLine( moBIECmd ).execute( "--help" );
		else
			new CommandLine( moBIECmd ).setCaseInsensitiveEnumValuesAllowed( true ).execute( args );
	}
}