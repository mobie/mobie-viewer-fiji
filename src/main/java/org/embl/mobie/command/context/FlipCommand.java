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

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.DataStore;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.transform.TransformHelper;
import org.jetbrains.annotations.NotNull;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Flip")
public class FlipCommand extends AbstractTransformationCommand
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Axis", choices = {"X", "Y", "Z"} )
	public String axis;

	@Parameter ( label = "Apply", callback = "applyTransform" )
	public Button applyTransform;

	@Override
	public void initialize()
	{
		super.initialize();

		getInfo().getMutableInput( "transformationName", String.class )
				.setValue( this, "Flip transformation" );
	}

	public void applyTransform()
	{
		AffineTransform3D transform = createFlipTransform( movingSac );
		applyTransform( transform, "Flip " + axis + " axis" );
	}

	@NotNull
	private AffineTransform3D createFlipTransform( SourceAndConverter< ? > sourceAndConverter )
	{
		AffineTransform3D flip = new AffineTransform3D();
		switch ( axis )
		{
			case "X":
				flip.set(-1, 0, 0);
				break;
			case "Y":
				flip.set(-1, 1, 1);
				break;
			case "Z":
				flip.set(-1, 2, 2);
				break;
		}

		AffineTransform3D transform = new AffineTransform3D();
		double[] center = TransformHelper.getCenter( DataStore.sourceToImage().get( sourceAndConverter ), 0 );
		transform.translate( Arrays.stream( center ).map( x -> -x ).toArray() );
		transform.preConcatenate( flip );
		transform.translate( center );
		return transform;
	}
}
