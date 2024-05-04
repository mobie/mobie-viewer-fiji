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

import loci.common.DebugTools;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.transform.GridType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Table..." )
public class OpenTableCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Table Path", required = true )
	public File table;

	@Parameter( label = "Image Path Columns (Comma Separated)", required = true )
	public String images;

	@Parameter( label = "Labels Path Columns (Comma Separated)", required = false )
	public String labels;

	@Parameter( label = "Images & Labels Root Folder",
			style = "directory",
			description = "Use this is if the images and labels paths in the table are relative.",
			required = false )
	public File root;

	@Parameter( label = "Path Mapping (From,To)",
			description = "If the data was analysed on a different computer.\n" +
					"For example from Linux to MacOS: \"/g,/Volumes\"",
			required = false )
	public String pathMapping;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

	@Override
	public void run()
	{
		run( GridType.Stitched );
	}

	public void run( GridType gridType )
	{
		DebugTools.setRootLevel( "OFF" );

		final MoBIESettings settings = new MoBIESettings();
		settings.removeSpatialCalibration( removeSpatialCalibration );

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
