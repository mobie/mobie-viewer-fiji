/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
import org.embl.mobie.lib.transform.GridType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Multiple Images and Labels..." )
public class OpenMultipleImagesAndLabelsCommand implements Command {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Image Path", required = false )
	public File image0;

	@Parameter( label = "Image Path", required = false )
	public File image1;

	@Parameter( label = "Image Path", required = false )
	public File image2;

	@Parameter( label = "Image Path", required = false )
	public File image3;

	@Parameter( label = "Labels Path", required = false )
	public File labels0;

	@Parameter( label = "Labels Table Path", required = false )
	public File table0;

	@Parameter( label = "Labels Path", required = false )
	public File labels1;

	@Parameter( label = "Labels Table Path", required = false )
	public File table1;

	@Parameter( label = "Remove Spatial Calibration", required = false )
	public Boolean removeSpatialCalibration = false;

	@Override
	public void run()
	{
		final GridType gridType = GridType.Stitched; // TODO: fetch from UI

		final ArrayList< String > imageList = new ArrayList<>();
		if ( image0 != null ) imageList.add( image0.getAbsolutePath() );
		if ( image1 != null ) imageList.add( image1.getAbsolutePath() );
		if ( image2 != null ) imageList.add( image2.getAbsolutePath() );
		if ( image3 != null ) imageList.add( image3.getAbsolutePath() );

		final ArrayList< String > labelsList = new ArrayList<>();
		if ( labels0 != null ) labelsList.add( labels0.getAbsolutePath() );
		if ( labels1 != null ) labelsList.add( labels1.getAbsolutePath() );

		final ArrayList< String > tablesList = new ArrayList<>();
		if ( table0 != null ) tablesList.add( table0.getAbsolutePath() );
		if ( table1 != null ) tablesList.add( table1.getAbsolutePath() );

		final MoBIESettings settings = new MoBIESettings();
		settings.removeSpatialCalibration( removeSpatialCalibration );
		String groovyCode = generateGroovyScript();
		settings.appendGroovyCode( groovyCode );
//		System.out.println( groovyCode );

		try
		{
			new MoBIE( imageList, labelsList, tablesList, null, gridType, settings );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	public String generateGroovyScript() {
		StringBuilder sb = new StringBuilder();

		sb.append("import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand\n");
		sb.append("import java.io.File\n\n");

		sb.append("OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand()\n");

		if (image0 != null) sb.append(String.format("command.image0 = new File(\"%s\")\n",
				image0.getAbsolutePath()));
		if (image1 != null) sb.append(String.format("command.image1 = new File(\"%s\")\n",
				image1.getAbsolutePath()));
		if (image2 != null) sb.append(String.format("command.image2 = new File(\"%s\")\n",
				image2.getAbsolutePath()));
		if (image3 != null) sb.append(String.format("command.image3 = new File(\"%s\")\n",
				image3.getAbsolutePath()));

		if (labels0 != null) sb.append(String.format("command.labels0 = new File(\"%s\")\n",
				labels0.getAbsolutePath()));
		if (labels1 != null) sb.append(String.format("command.labels1 = new File(\"%s\")\n",
				labels1.getAbsolutePath()));

		if (table0 != null) sb.append(String.format("command.table0 = new File(\"%s\")\n",
				table0.getAbsolutePath()));
		if (table1 != null) sb.append(String.format("command.table1 = new File(\"%s\")\n",
				table1.getAbsolutePath()));

		if (removeSpatialCalibration != null) {
			sb.append("command.removeSpatialCalibration = ").append(removeSpatialCalibration).append("\n");
		}

		sb.append("\ncommand.run()\n");

		return sb.toString();
	}
}
