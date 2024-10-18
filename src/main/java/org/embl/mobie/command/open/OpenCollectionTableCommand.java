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
import org.embl.mobie.ProjectType;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Collection Table..." )
public class OpenCollectionTableCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static enum DataRoot{
		PathsInTableAreAbsolute,
		UseTableFolder,
		UseBelowDataRootFolder
	}

	@Parameter( label = "Table Path" )
	public File table;

	@Parameter( label = "Data Root",
			description = "Specify whether the data URIs in the table are absolute or relative." )
	public DataRoot dataRoot = DataRoot.PathsInTableAreAbsolute;

	@Parameter( label = "( Data Root Folder )",
			style = "directory",
			description = "Optional. Use this is if the paths to the images and labels in the table are relative.",
			required = false )
	public File dataRootFile;

	@Parameter( label = "Viewing mode",
			description = "Volumetric viewing enables arbitrary plane slicing. Planar viewing mode will restrict browsing to the XY, YZ, or XZ planes.",
			required = false )
	public BdvViewingMode bdvViewingMode = BdvViewingMode.ThreeDimensional;


	@Override
	public void run()
	{
		DebugTools.setRootLevel( "OFF" );

		String dataRootString;
		switch ( dataRoot )
		{
			case UseBelowDataRootFolder:
				dataRootString = dataRootFile == null ? null : dataRootFile.getAbsolutePath();
				break;
			case UseTableFolder:
				dataRootString = table.getParent();
				break;
			case PathsInTableAreAbsolute:
			default:
				dataRootString = null;
				break;
		}

		final MoBIESettings settings = new MoBIESettings()
				.projectType( ProjectType.CollectionTable )
				.dataRoot( dataRootString )
				.bdvViewingMode( bdvViewingMode );

		try
		{
			new MoBIE( MoBIEHelper.toURI( table ), settings );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}
