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
package projects.em_xray_alignment;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;
import java.io.IOException;

public class OpenEmXraySlices
{
    public static void main( String[] args ) throws IOException
    {
       // OpenerLogging.setLogging( true );
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
        command.image0 = new File("/Users/tischer/Desktop/em-xray/xray-slice-ds-0.tif");
        command.image1 = new File("/Users/tischer/Desktop/em-xray/em-slice-ds-0.tif");
        command.run();
//        AutomaticRegistrationCommand automaticRegistrationCommand = new AutomaticRegistrationCommand();
//        automaticRegistrationCommand.bdvHandle = MoBIE.getInstance().getViewManager().getSliceViewer().getBdvHandle();
//        automaticRegistrationCommand.run();
    }
}
