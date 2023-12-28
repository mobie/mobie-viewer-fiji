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
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.CommandConstants;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - SIFT")
public class SIFT2DAlignCommand implements BdvPlaygroundActionCommand, Interactive
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter
	public BdvHandle bdvHandle;

	@Parameter ( label = "Compute Alignment", callback = "compute")
	private Button compute;

	@Parameter ( label = "Toggle Alignment", callback = "toggle")
	private Button toggle;

	private AffineTransform3D previousTransform;
	private AffineTransform3D newTransform;
	private TransformedSource< ? > transformedSource;
	private boolean isAligned;

	@Override
	public void run()
	{
		//
	}

	private void toggle()
	{
		if ( transformedSource == null )
		{
			IJ.showMessage( "Please first [ Compute Alignment ]." );
			return;
		}

		if ( isAligned )
		{
			transformedSource.setFixedTransform( previousTransform );
		}
		else
		{
			transformedSource.setFixedTransform( newTransform );
		}

		bdvHandle.getViewerPanel().requestRepaint();
		isAligned = ! isAligned;
	}

	private void compute()
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
			AffineTransform3D siftTransform = aligner.getSiftTransform3D();
			transformedSource = ( TransformedSource< ? > ) movingSac.getSpimSource();
			previousTransform = new AffineTransform3D();
			transformedSource.getFixedTransform( previousTransform );
			newTransform = previousTransform.copy();
			newTransform.preConcatenate( siftTransform );
			transformedSource.setFixedTransform( newTransform );
			IJ.showMessage( "Transforming " + transformedSource.getName() );
			IJ.showMessage( "Previous Transform: " + previousTransform );
			IJ.showMessage( "Additional SIFT Transform: " + siftTransform );
			IJ.showMessage( "Combined Transform: " + newTransform );

			isAligned = true;
			bdvHandle.getViewerPanel().requestRepaint();
		}
		else
		{
			IJ.log("Cannot apply transformation to image of type " + movingSac.getSpimSource().getClass() );
		}
	}


}
