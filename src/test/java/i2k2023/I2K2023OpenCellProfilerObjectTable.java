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
package i2k2023;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

class I2K2023OpenCellProfilerObjectTable
{
	public static void main( String[] args ) throws Exception
	{
		new ImageJ().ui().showUI();
		final OpenTableCommand command = new OpenTableCommand();
		command.root = new File( "/Users/tischer/Documents/cellprofiler-practical-NeuBIAS-Lisbon-2017/mobie" );
		command.table = new File( "/Users/tischer/Documents/cellprofiler-practical-NeuBIAS-Lisbon-2017/mobie/Cells.txt" );
		command.images = "FileName_DNA=DNA,FileName_PLA=PLA";
		command.labels = "FileName_CellLabels=Cells,FileName_NucleiLabels=Nuclei,FileName_PLALabels=PLASpots";
		command.removeSpatialCalibration = true;
		command.run();

		//MoBIE.getInstance().getViewManager().show( "Nuclei" );
	}
}
