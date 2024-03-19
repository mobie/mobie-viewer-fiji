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
package develop;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableAdvancedCommand;
import org.embl.mobie.lib.transform.GridType;

import java.io.File;

public class OpenAutoMicTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        OpenTableAdvancedCommand command = new OpenTableAdvancedCommand();
        //command.table = new File( "/Volumes/almf/group/Aliaksandr/User_data/Furlong_CrispR/test_data_20231018/20231004/20231004-172458/summary_calculated1.txt" );
        //command.table = new File( "/Volumes/almf/group/Aliaksandr/User_data/Furlong_CrispR/test_data_20231018/20231004/20231004-172458/summary_calculated1_subset.txt" );
        command.table = new File( "/Users/tischer/Desktop/teresa/summary_calculated1_subset.txt" );
        //command.table = new File( "/Volumes/cba/exchange/furlong_test/summary_calculated1_subset_zarr.txt" );
        //command.images = "Result.Image.Zarr"; // Result.Image.Zarr
        command.root = command.table.getParentFile();
        command.images = "FileName_Result.Image_IMG"; // Result.Image.Zarr
        command.gridType = GridType.Transformed; // FIXME: not working with Stitched!
        command.run();
    }
}
