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
package org.embl.mobie.command.context;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.YesNoCancelDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.CommandConstants;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - SIFT")
public class SIFT2DAlignCommand implements BdvPlaygroundActionCommand
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter
	public BdvHandle bdvHandle;

	@Override
	public void run()
	{
		IJ.log("# SIFT registration" +
				"\nThe registration is computed in the currently visible 2D plane" +
				"\nand then applied to the full image in 3D.");
		// start the alignment, which has its own GUI
		SIFT2DAligner aligner = new SIFT2DAligner( bdvHandle );
		if( ! aligner.showUI() ) return;

		// apply transformation
		SourceAndConverter< ? > movingSac = aligner.getMovingSac();
		if ( movingSac.getSpimSource() instanceof TransformedSource )
		{
			// apply the transformation
			AffineTransform3D siftTransform3D = aligner.getSiftTransform3D();
			TransformedSource< ? > transformedSource = ( TransformedSource< ? > ) movingSac.getSpimSource();
			AffineTransform3D previousFixedTransform = new AffineTransform3D();
			transformedSource.getFixedTransform( previousFixedTransform );
			AffineTransform3D newFixedTransform = previousFixedTransform.copy();
			newFixedTransform.preConcatenate( siftTransform3D );
			transformedSource.setFixedTransform( newFixedTransform );
			bdvHandle.getViewerPanel().requestRepaint();

			// ask user whether to accept the transformation
			NonBlockingGenericDialog dialog = new NonBlockingGenericDialog( "SIFT Alignment" );
			dialog.addMessage( "Press OK to keep the transformation" );
			dialog.addMessage( "Press Cancel to discard the transformation" );
			dialog.showDialog();
			if ( dialog.wasOKed() )
			{
				IJ.log( "Transformed " + movingSac.getSpimSource().getName() + " with " + siftTransform3D );
			}
			else if ( dialog.wasCanceled() )
			{
				transformedSource.setFixedTransform( previousFixedTransform );
				bdvHandle.getViewerPanel().requestRepaint();
			}
		}
		else
		{
			IJ.log("Cannot apply transformation to image of type " + movingSac.getSpimSource().getClass() );
		}
	}
}
