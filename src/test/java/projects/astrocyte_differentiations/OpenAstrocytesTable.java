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
package projects.astrocyte_differentiations;

import net.imagej.ImageJ;
import org.embl.mobie.command.SpatialCalibration;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

class OpenAstrocytesTable
{
	public static void main( String[] args ) throws Exception
	{
		/*
		mounting on Mac:
		cmd K
		cifs://heard/heard/a_ritz/Microscopy/20231101_CR_AD_IF_GFAPki67/
		cifs://heard/heard/a_ritz/Microscopy/20231106_CRADI3_4d_IF

		 */

		new ImageJ().ui().showUI();
		final OpenTableCommand command = new OpenTableCommand();
		command.root = new File( "/Volumes/20231101_CR_AD_IF_GFAPki67/analysis" );
		command.table = new File( "/Volumes/20231101_CR_AD_IF_GFAPki67/analysis/concatenated.tsv" );
		command.images = "DAPI_Path=DAPI,ki67_Path=ki67,GFAP_Path=GFAP";
		command.labels = "Nuclei_Labels_Path=Nuclei,Nuclei_Periphery_Labels_Path=Periphery";
		command.spatialCalibration = SpatialCalibration.UsePixelUnits;
		command.run();
	}
}
