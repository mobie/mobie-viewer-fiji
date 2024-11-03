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
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.ui.UserInterfaceHelper;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open OpenOrganelle" )
public class OpenOpenOrganelleDataCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Override
	public void run()
	{
		final URL resource = OpenOpenOrganelleDataCommand.class.getResource( "/open-organelle.txt" );
		String file = resource.getFile();
		OpenCollectionTableCommand command = new OpenCollectionTableCommand();
		command.table = new File( file );
		command.run();
	}

	public static void main( String[] args )
	{
		// to update the table use org.embl.mobie.lib.util.OpenOrganelleCollectionTableCreator

		new ImageJ().ui().showUI();
		new OpenOpenOrganelleDataCommand().run();

		// TODO
		// - [X] The grid overlay boundaries are too thick for some reason
		// - [X] Auto-contrast in a Transformed Grid View should be determined from an image that is currently visible!
		// - [ ] Auto-contrast in the beginning of a grid view display would be nice
		// - [X] For each RegionDisplay automatically overlay the region_id as an annotation
		// - [X] The grids should not be exclusive in this case
		// - [X] The EM should show first
		// - [ ] How to implement one Grid and RegionDisplay for many sources?
		//     - how to deal with the fact that then there are multiple table rows for each region?
		// - [ ] It would be cleaner if all the layers would be part of the same grid
		//     - this could be achieved by adding a "grid_position" column
		//     - but then we would also need a "display" column to sort the entries into the respective displays
		//     - an issue with this would be how to implement the region display, because s.a.
		// - [X] Zoom to region when clicking on a RegionTable row
		// - [ ] Yannick: exclusive column
		// - [ ] John: Membrane segmentation pixel shit in jrc_jurkat-1/labels/pm_seg

	}
}
