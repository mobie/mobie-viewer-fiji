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
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.data.ProjectType;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open MoBIE Collection Table..." )
public class OpenCollectionTableCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); DebugTools.setRootLevel( "OFF" ); }

	@Parameter( label = "Table Uri",
			description = "Location of the table. Supported formats: TSV, CSV, Excel, Google Sheet URLs")
	public String tableUri;

	@Parameter ( label = "( S3 Access Key )",
			description = "Optional. Access key for a protected S3 bucket.",
			persist = false,
			required = false )
	public String s3AccessKey;

	@Parameter ( label = "( S3 Secret Key )",
			description = "Optional. Secret key for a protected S3 bucket.",
			persist = false,
			required = false )
	public String s3SecretKey;

	@Override
	public void run()
	{
		// NB: this is unfortunately necessary to support both the GUI and the helpless execution of this command.
		//bdvViewingModeEnum = bdvViewingModeEnum == null ? BdvViewingMode.valueOf( bdvViewingMode ) : bdvViewingModeEnum;

		final MoBIESettings settings = new MoBIESettings()
				.projectType( ProjectType.CollectionTable )
				.dataRoot( IOHelper.getParentLocation( tableUri ) );

		if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
			settings.s3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } );

		openTable( tableUri, settings );
	}

	protected void openTable( String tableUri, MoBIESettings settings )
	{
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
