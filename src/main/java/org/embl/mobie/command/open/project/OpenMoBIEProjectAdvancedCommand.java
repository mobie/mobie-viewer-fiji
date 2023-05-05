/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.command.open.project;

import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.io.ImageDataFormatNames;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.io.ImageDataFormat;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN_PROJECT + "Open MoBIE Project Expert Mode..." )
public class OpenMoBIEProjectAdvancedCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String projectLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Project Branch" )
	public String projectBranch = "master";

	@Parameter ( label = "Image Data Storage Modality", choices = { ImageDataFormatNames.BDVN5, ImageDataFormatNames.BDVN5S3, ImageDataFormatNames.BDVOMEZARR, ImageDataFormatNames.BDVOMEZARRS3, ImageDataFormatNames.OMEZARR, ImageDataFormatNames.OMEZARRS3, ImageDataFormatNames.OPENORGANELLES3 } )
	public String imageDataStorageModality = ImageDataFormatNames.OMEZARRS3;

	@Parameter ( label = "Image Data Location" )
	public String imageDataLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Table Data Location" )
	public String tableDataLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Table Data Branch" )
	public String tableDataBranch = "master";

	@Override
	public void run()
	{
		try
		{
			new MoBIE(
					projectLocation,
					MoBIESettings.settings()
							.gitProjectBranch( projectBranch )
							.addImageDataFormat( ImageDataFormat.valueOf( imageDataStorageModality ) )
							.imageDataLocation( imageDataLocation )
							.tableDataLocation( tableDataLocation )
							.gitTablesBranch( tableDataBranch ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}


}
