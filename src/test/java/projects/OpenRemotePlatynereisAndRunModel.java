/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package projects;

import bdv.util.BdvHandle;
import ij.IJ;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.ModelRunnerCommand;
import org.embl.mobie.lib.MoBIE;

import java.io.IOException;

public class OpenRemotePlatynereisAndRunModel
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/platybrowser/platybrowser");
		final BdvHandle bdvHandle = moBIE.getViewManager().getSliceViewer().getBdvHandle();

		// Focus on some smaller area within the volume
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( 31.524161974149372,0.0,0.0,-3471.2941398257967,0.0,31.524161974149372,0.0,-3335.2908913145466,0.0,0.0,31.524161974149372,-4567.901470761989 );
		bdvHandle.getViewerPanel().state().setViewerTransform( viewerTransform );
		IJ.wait( 1000 );

		// run the model runner command
		final ModelRunnerCommand command = new ModelRunnerCommand();
		command.bdvHandle = bdvHandle;
		command.run();
	}
}
