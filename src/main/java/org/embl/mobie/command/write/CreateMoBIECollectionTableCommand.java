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
package org.embl.mobie.command.write;

import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.embl.mobie.lib.create.CollectionTableCreator;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import tech.tablesaw.api.Table;

import java.io.File;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Create>Create MoBIE Collection Table..." )
public class CreateMoBIECollectionTableCommand implements Command {

    public static final String TOGETHER = "Together";
    public static final String INDIVIDUAL = "Individual";
    public static final String GRID = "Grid";

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter( label= "Image files",
                description = "The image files that will be part of the collection table.")
    public File[] files;

    @Parameter( label= "( Filename regular expression )",
                description = "Optional. Regular expression to extract information from file names." +
                        "\nThe extracted information will be added to the table as additional columns." +
                        "\nIt can also be used to configure the grid layout (see below).")
    public String regExp = "(?<condition>.*)--(?<replicate>.*).tif";

    @Parameter( label= "View layout", choices = { TOGETHER, INDIVIDUAL, GRID },
            description = "Specifies how the data will be displayed in MoBIE."
                    + "\nTogether: All images in one view on top of each other (for correlative data)"
                    + "\nIndividual: Each image is a separate view (for unrelated data of different dimensionality)"
                    + "\nGrid: All images in one view in a grid (for comparing various experimental conditions, with same microscopy settings)" )
    public String viewLayout = GRID;

//    @Parameter( label= "( Grid axes )",
//            description = "Optional. The axes of the grid can be configured using the metadata that is extracted with the above regular expression." +
//                    "\nExamples:" +
//                    "\nx=replicate,y=condition" +
//                    "\ny=condition" )
//    public String gridAxes = "x=replicate,y=condition";

//    @Parameter( label = "Convert to OME-Zarr",
//            description = "")
//    public Boolean convertToZarr;

    @Parameter( label = "Output table",
                description = "The collection table file.")
    public File outputTableFile;

    @Parameter( label = "Open table in MoBIE",
            description = "Immediately open the collection table file with MoBIE.")
    public Boolean openTableInMoBIE;

    @Override
    public void run()
    {
        CollectionTableCreator tableCreator =
                new CollectionTableCreator( files, outputTableFile, viewLayout, regExp );
        Table table = tableCreator.createTable();
        outputTableFile.getParentFile().mkdirs();
        table.write().csv( outputTableFile );

        if ( openTableInMoBIE )
        {
            OpenCollectionTableCommand openCommand = new OpenCollectionTableCommand();
            openCommand.tableUri = outputTableFile.getAbsolutePath();
            openCommand.dataRootTypeEnum = OpenCollectionTableCommand.DataRootType.PathsInTableAreAbsolute;
            openCommand.bdvViewingModeEnum = BdvViewingMode.ThreeDimensional;
            openCommand.run();
        }
    }
}
