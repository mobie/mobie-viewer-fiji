/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.cmd;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.lib.transform.GridType;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie-files", mixinStandardHelpOptions = true, version = "4.0.3", description = "Visualise multi-modal big image data files, see https://mobie.github.io/")
public class FilesCmd implements Callable< Void > {

	public static final String RC = "-rc";
	public static final String REMOVE_CALIBRATION = "--remove-calibration";
	public static final String R = "-r";
	public static final String ROOT = "--root";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = { R, ROOT }, required = false, description = "root folder that will be prepended to all other arguments")
	public String root;

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image path, e.g., \"/home/image.tif\"; use Java regular expressions to open several images, e.g., \"/home/.*-image.tif\"; specify several images by repeating the -i flag; use \"=\" to specify a name for the image, e.g., \"nuclei=/home/image.tif\"; use \";\" to specify loading only one channel from a multichannel file, e.g., \"nuclei=/home/image.tif;0\" ")
	public String[] images;

	@Option(names = {"-l", "--labels"}, required = false, description = "label mask image path, e.g. -l \"/home/labels.tif\"; see --image for more details")
	public String[] labels;

	@Option(names = {"-t", "--table"}, required = false, description = "segment feature table path, matching the label image")
	public String[] tables;

	@Option(names = {"-g", "--grid"}, required = false, description = "grid type: none, stitched (default), transform")
	public GridType gridType = GridType.Stitched;

	// FIXME: This is wrong now
	@Option(names = { RC, REMOVE_CALIBRATION }, required = false, description = "flag to remove spatial calibration from all images; this can be useful if only some images have a spatial calibration metadata and thus overlaying several images would fail")
	public SpatialCalibration spatialCalibration = SpatialCalibration.FromImageFiles;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings();

		settings.openedFromCLI( true );

		spatialCalibration.setVoxelDimensions( settings, tables != null ? tables[ 0 ] : null );

		List< String > imageList = images != null ?
				Arrays.asList( images ) : new ArrayList<>();

		List< String > labelsList = labels != null ?
				Arrays.asList( labels ) : new ArrayList<>();

		List< String > tablesList = tables != null ?
				Arrays.asList( tables ) : new ArrayList<>();

		new MoBIE( imageList, labelsList, tablesList, root, gridType, settings );

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
