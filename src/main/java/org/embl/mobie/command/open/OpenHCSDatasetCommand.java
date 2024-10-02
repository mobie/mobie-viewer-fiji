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

import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.hcs.OMEXMLParser;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.io.File;
import java.io.IOException;

import static org.embl.mobie.command.open.OpenHCSDatasetCommand.VoxelDimensionFetching.FromOMEXML;


@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open HCS Dataset..." )
public class OpenHCSDatasetCommand implements Command
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "HCS Plate Directory", style = "directory")
	public File hcsDirectory;

	@Parameter ( label = "Relative Well Margin" )
	public double wellMargin = 0.1;

	@Parameter ( label = "Relative Site Margin" )
	public double siteMargin = 0.0;

	@Parameter ( label = "Help", callback = "help")
	public Button help;

	public enum VoxelDimensionFetching
	{
		FromImageFiles,
		FromOMEXML
	}

	@Parameter ( label = "Voxel Dimensions" )
	public VoxelDimensionFetching voxelDimensionFetching = VoxelDimensionFetching.FromImageFiles;

	@Parameter ( label = "( OME-XML)",
			description = "Optional. This is used if the option FromOMEXML is chosen to for" +
					" determining the Voxel Dimensions",
			persist = false, required = false )
	public File omeXML;

	@Override
	public void run()
	{
		VoxelDimensions voxelDimensions = initVoxelDimensions();

		try
		{
			new MoBIE( MoBIEHelper.toURI( hcsDirectory ), new MoBIESettings(), wellMargin, siteMargin, voxelDimensions );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	private VoxelDimensions initVoxelDimensions()
	{
		if ( voxelDimensionFetching.equals( FromOMEXML ) )
		{
			return OMEXMLParser.readVoxelDimensions( omeXML );
		}
		else
		{
			return null;
		}
	}

	private void help()
	{
		IOHelper.openURI( "https://mobie.github.io/tutorials/hcs.html" );
	}

}
