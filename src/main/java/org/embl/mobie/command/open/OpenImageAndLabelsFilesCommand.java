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
package org.embl.mobie.command.open;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Image and Labels Files..." )
public class OpenImageAndLabelsFilesCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	// FIXME: https://forum.image.sc/t/scijava-ui-open-both-file-and-directory/97389
	@Parameter( label = "Image Path", style = "both", required = false )
	public File image;

	@Parameter( label = "Label Mask Path", style = "both", required = false )
	public File labels;

	@Parameter( label = "Label Mask Table Path", required = false )
	public File table;

	@Parameter( label = "Spatial Calibration" )
	public SpatialCalibration spatialCalibration = SpatialCalibration.FromImage;

	@Parameter( label = "Grid type", description = MoBIEHelper.GRID_TYPE_HELP )
	public GridType gridType = GridType.Transformed;

	@Override
	public void run()
	{
		final MoBIESettings settings = new MoBIESettings();

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image != null ) imageList.add( image.getAbsolutePath() );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels != null ) labelsList.add( labels.getAbsolutePath() );

		final ArrayList< String > tablesList = new ArrayList<>();
		if ( table != null )
		{
			tablesList.add( table.getAbsolutePath() );
		}

		spatialCalibration.setVoxelDimensions( settings, table != null ? table.getAbsolutePath() : null );

		try
		{
			new MoBIE( imageList, labelsList, tablesList, null, gridType, settings );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

}
