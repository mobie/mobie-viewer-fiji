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
package org.embl.mobie.command.context;

import net.imglib2.realtransform.AffineTransform3D;
import org.checkerframework.checker.units.qual.C;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.ui.UserInterfaceHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>" + EnterTransformationCommand.COMMAND_NAME )
public class EnterTransformationCommand extends AbstractTransformationCommand
{
	public static final String COMMAND_NAME = "Registration - Enter Transformation";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Transformation 3D affine" )
	public String transformation = Arrays.toString( new AffineTransform3D().getRowPackedCopy() );

	@Parameter ( label = "Apply transform", callback = "applyTransform" )
	public Button applyTransform;


	@Override
	public void initialize()
	{
		super.initialize();

		getInfo().getMutableInput( "suffix", String.class )
				.setValue( this, "et");
	}

	private void applyTransform()
	{
		setMovingImages();

		AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.set( parseStringToDoubleArray( transformation ) );
		applyTransform( affineTransform3D );
		UserInterfaceHelper.closeWindowByName( COMMAND_NAME );
	}


	public static double[] parseStringToDoubleArray(String arrayStr)
	{
		arrayStr = arrayStr.replaceAll("\\[|\\]", "");
		String[] items = arrayStr.split(",\\s*");
        return Arrays.stream(items).mapToDouble(Double::parseDouble).toArray();
	}
}
