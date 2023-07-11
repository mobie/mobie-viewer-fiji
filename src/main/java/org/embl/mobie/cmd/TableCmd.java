/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import static org.embl.mobie.cmd.FilesCmd.R;
import static org.embl.mobie.cmd.FilesCmd.RC;
import static org.embl.mobie.cmd.FilesCmd.REMOVE_CALIBRATION;
import static org.embl.mobie.cmd.FilesCmd.ROOT;

// TODO: Derive this class from a base class (common with mobie-files and mobie-hcs)
@CommandLine.Command(name = "mobie-table", mixinStandardHelpOptions = true, version = "4.0.3", description = "Visualise images and labels masks from a dataset table, see https://mobie.github.io/")
public class TableCmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = {"-t", "--table"}, required = true, description = "dataset table, each row corresponding to one image or to one object in an image")
	public String table;

	@Option(names = {R, ROOT}, required = false, description = "root folder that will be prepended to all image and label column entries")
	public String root;

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image column; to open multiple columns repeat the -i parameter; to replace the column name use, e.g., \"Nuclei=FilePath_DAPI\"; to only open one channel from a multi-channel file use, e.g., \"Nuclei=FilePath_Image;0\"")
	public String[] images;

	@Option(names = {"-l", "--labels"}, required = false, description = "label mask image column; if the table is an object table and contains multiple label mask columns the first label column must be the one that corresponds to the label index and anchor columns (this is, e.g., relevant for CellProfiler object tables where one can add information about other (child) objects to the parent object); see --images for further explanations")
	public String[] labels;

	@Option(names = {"-g", "--grid"}, required = false, description = "grid type: none, stitched (default), transform; \"stitched\" should yield the best performance but requires that all images have the same dimensions")
	public GridType gridType = GridType.Stitched;

	@Option(names = {RC, REMOVE_CALIBRATION}, required = false, description = "removes spatial calibration from all images; this is useful if only some images have a spatial calibration and thus the overlay would fail.")
	public Boolean removeSpatialCalibration = false;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings()
				.openedFromCLI( true )
				.removeSpatialCalibration( removeSpatialCalibration );

		List< String > imageList = images != null ?
				Arrays.asList( images ) : new ArrayList<>();

		List< String > labelsList = labels != null ?
				Arrays.asList( labels ) : new ArrayList<>();

		new MoBIE( table, imageList, labelsList, root, gridType, settings );

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
