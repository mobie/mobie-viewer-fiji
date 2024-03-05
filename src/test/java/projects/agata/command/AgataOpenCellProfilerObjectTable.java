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
package projects.agata.command;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

class AgataOpenCellProfilerObjectTable
{
	public static void main( String[] args ) throws Exception
	{
		//command.root = new File( "/g/cba/exchange/agata-misiaszek/data/analysed" );
		//command.table = new File( "/g/cba/exchange/agata-misiaszek/data/analysed/Nuclei.txt" );

		new ImageJ().ui().showUI();
		final OpenTableCommand command = new OpenTableCommand();
		command.root = new File( "/Users/tischer/Desktop/mobie-data/cellprofiler" );
		command.table = new File( "/Users/tischer/Desktop/mobie-data/cellprofiler/Nuclei.txt" );
		command.images = "Image_FileName_DNA=DAPI;0,Image_FileName_DNA=RPAC1;1";
		command.labels = "Image_FileName_NucleiLables=Nuclei,Image_FileName_NucleoplasmLabels=Nucleoplasm,Image_FileName_NucleoliLabels=Nucleoli,Image_FileName_SpecklesLabels=Speckles";
		command.removeSpatialCalibration = true;
		command.run();

		MoBIE.getInstance().getViewManager().show( "Nuclei" );
	}
}
