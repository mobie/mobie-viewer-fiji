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
package org.embl.mobie.command.open.special;

import loci.common.DebugTools;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Special>Open Microglia Morphometry Table..." )
public class OpenMicrogliaTableCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Table", required = true )
	public File table;

	@Parameter(visibility = ItemVisibility.MESSAGE,
			persist = false,
			required = false )
	private String message = "Opens tables created with the Microglia-Morphometry plugin.";

	@Parameter(visibility = ItemVisibility.MESSAGE,
			persist = false,
			required = false)
	private String info = "More info: https://github.com/embl-cba/microglia-morphometry";


	private String images = "Path_Intensities,Path_Skeletons,Path_Annotations";

	private String labels = "Path_LabelMasks";

	private File root;

	private String pathMapping;

	private SpatialCalibration spatialCalibration = SpatialCalibration.FromImage;

	private GridType gridType = GridType.Transformed;

	@Override
	public void run()
	{
		run( gridType );
	}

	private void run( GridType gridType )
	{
		DebugTools.setRootLevel( "OFF" );

		final MoBIESettings settings = new MoBIESettings();

		spatialCalibration.setVoxelDimensions( settings, table != null ? table.getAbsolutePath() : null  );
		root = table.getParentFile();

		List< String > imageList = new ArrayList<>();
		if ( images != null && ! images.equals( "" ) )
		{
			imageList = Arrays.asList( images.split( "," ) );
			imageList = imageList.stream().map( s -> s.trim() ).collect( Collectors.toList() );
		}

		List< String > labelList = new ArrayList<>();
		if ( labels != null && ! labels.equals( "" ) )
		{
			labelList = Arrays.asList( labels.split( "," ) );
			labelList = labelList.stream().map( s -> s.trim() ).collect( Collectors.toList() );
		}

		try
		{
			String rootPath = root == null ? null : root.getAbsolutePath();
			new MoBIE( table.getAbsolutePath(), imageList, labelList, rootPath, pathMapping ,gridType, settings );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
