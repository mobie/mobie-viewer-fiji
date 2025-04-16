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
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.embl.mobie.lib.data.ProjectType;
import org.embl.mobie.lib.util.GoogleSheetURLHelper;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Special > Open Simple Collection Table..." )
public class OpenSimpleCollectionTableCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); DebugTools.setRootLevel( "OFF" ); }

	@Parameter( label = "Table Uri" )
	public String tableUri;

	@Override
	public void run()
	{
		final MoBIESettings settings = new MoBIESettings()
				.projectType( ProjectType.CollectionTable );

		openTable( tableUri, settings );
	}

	protected void openTable( String tableUri, MoBIESettings settings )
	{
		if ( tableUri.contains( "docs.google.com/spreadsheets" ) )
			tableUri = GoogleSheetURLHelper.generateExportUrl( tableUri );

		try
		{
			new MoBIE( tableUri, settings );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}
}
