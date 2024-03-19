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
package projects.colony_detection_anavo;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenColonyWellTableWithLabels
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenTableCommand command = new OpenTableCommand();
        //command.root = new File( "/Users/tischer/Desktop/moritz/CQ1_testfiles-wells" );
        //command.table = new File( "/Users/tischer/Desktop/moritz/CQ1_testfiles-wells/well_table_with_colonies.csv" );
        command.root = new File( "/Users/tischer/Desktop/moritz/U2OS_subset-wells" );
        command.table = new File( "/Users/tischer/Desktop/moritz/U2OS_subset-wells/well_table_with_colonies.csv" );
        command.images = "file_name";
        command.labels = "labels_file_name";
        command.removeSpatialCalibration = true;
        command.run();
    }
}
