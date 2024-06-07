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

import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Multiple Images and Label URIs..." )
public class OpenMultipleImagesAndLabelsURIsCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image URI", required = false )
	public String image0;

	@Parameter( label = "Image URI", required = false )
	public String image1;

	@Parameter( label = "Image URI", required = false )
	public String image2;

	@Parameter( label = "Image URI", required = false )
	public String image3;

	@Parameter( label = "Labels URI", required = false )
	public String labels0;

	@Parameter( label = "Labels Table URI", required = false )
	public String table0;

	@Parameter( label = "Labels URI", required = false )
	public String labels1;

	@Parameter( label = "Labels Table URI", required = false )
	public String table1;

	@Parameter( label = "Spatial Calibration" )
	public SpatialCalibration spatialCalibration = SpatialCalibration.FromImage;

	@Parameter( label = "Grid type", description = MoBIEHelper.GRID_TYPE_HELP )
	public GridType gridType = GridType.Transformed;

	@Override
	public void run()
	{
		final MoBIESettings settings = new MoBIESettings();

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image0 != null ) imageList.add( image0 );
		if ( image1 != null ) imageList.add( image1 );
		if ( image2 != null ) imageList.add( image2 );
		if ( image3 != null ) imageList.add( image3 );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels0 != null ) labelsList.add( labels0 );
		if ( labels1 != null ) labelsList.add( labels1 );

		final ArrayList< String > tablesList = new ArrayList<>();
		if ( table0 != null ) tablesList.add( table0 );
		if ( table1 != null ) tablesList.add( table1 );

		spatialCalibration.setVoxelDimensions( settings, table0 != null ? table0 : null );

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
