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

import ij.IJ;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.create.ui.ProjectsCreatorUI;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

import static org.embl.mobie.ui.UserInterfaceHelper.tidyString;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Create>Create MoBIE Collection Table..." )
public class CreateMoBIECollectionTableCommand implements Command {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter( label= "Files:",
                description = "The files that will be part of the collection table.")
    public File[] files;

    @Parameter( label= "( Filename regular expression )",
                description = "Optional. Regular expression to extract information from file names." +
                        "\nThe extracted information will be added to the table as additional columns." +
                        "\nIt can also be used to configure the grid layout (see below).")
    public String regExp = "(?<condition>.*)--(?<replicate>.*).*";

    @Parameter( label= "Grid layout", choices = {"Yes", "No"},
            description = "Whether to layout the data in a grid." +
                    "\nChoosing \"Yes\" mainly makes sense if all images are similar (e.g., same number of channels).")
    public String gridLayout = "Yes";

    @Parameter( label= "( Grid axes )",
            description = "Optional. The axes of the grid can be configured using the metadata that is extracted with the above regular expression." +
                    "\nExamples:" +
                    "\nx=replicate,y=condition"
                    "\ny=condition" )
    public String gridAxes = "x=replicate,y=condition";

    @Parameter( label = "Convert to OME-Zarr",
            description = "")
    public Boolean convertToZarr;

    @Parameter( label = "Output folder", style = "directory",
                description = "The folder where the collection table (and OME-Zarrs) will be saved.")
    public File outputFolder;


    @Override
    public void run()
    {
        IJ.showMessage( "Under development...please check again later" );
        return;

        outputFolder.mkdirs();

    }
}
