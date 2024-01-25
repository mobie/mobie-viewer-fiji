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
import org.embl.mobie.lib.MoBIEHelper;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Enter Transformation")
public class EnterTransformationCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Initializable
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter
	public BdvHandle bdvHandle;

	@Parameter ( label = "Image", choices = {""} )
	private String sourceName;

	@Parameter ( label = "3D Affine" )
	private String transformation = Arrays.toString( new AffineTransform3D().getRowPackedCopy() );
	private AffineTransform3D previousTransform;
	private TransformedSource< ? > transformedSource;

	public enum AdditionMode
	{
		Concatenate,
		Replace;
	}

	@Parameter ( label = "Addition Mode" )
	private AdditionMode mode = AdditionMode.Replace;

	@Parameter ( label = "Preview", callback = "apply" )
	private Button preview;

	private List< SourceAndConverter< ? > > sourceAndConverters;

	@Override
	public void initialize()
	{
		sourceAndConverters = MoBIEHelper.getVisibleSacs( bdvHandle );

		final List< String > imageNames = sourceAndConverters.stream()
				.map( sac -> sac.getSpimSource().getName() )
				.collect( Collectors.toList() );

		getInfo().getMutableInput( "sourceName", String.class )
				.setChoices( imageNames );
	}

	@Override
	public void run()
	{
		apply();
	}

	@Override
	public void cancel()
	{
		if ( transformedSource != null )
		{
			transformedSource.setFixedTransform( previousTransform );
			bdvHandle.getViewerPanel().requestRepaint();
		}
	}

	private void apply()
	{
		double[] doubles = parseStringToDoubleArray( transformation );
		AffineTransform3D additionalTransform = new AffineTransform3D();
		additionalTransform.set( doubles );

		if ( additionalTransform.isIdentity() )
			return;

		// TODO: the below logic will break when the
		// 	user decides to change the source name
		SourceAndConverter< ? > sourceAndConverter = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( sourceName ) )
				.findFirst().get();

		// FIXME: not sure I should transform the image?
		//   The issue is that I cannot undo it?
		//   Maybe I can by transforming it with the inverse?
//		Image< ? > image = DataStore.sourceToImage().get( sourceAndConverter );
//		image.transform(  );

		if ( sourceAndConverter.getSpimSource() instanceof TransformedSource )
		{
			transformedSource = ( TransformedSource< ? > ) sourceAndConverter.getSpimSource();

			if ( previousTransform != null )
			{
				// reset transform to initial state
				transformedSource.setFixedTransform( previousTransform );
			}
			else
			{
				// remember the previous transform
				// such that we can reset it
				previousTransform = new AffineTransform3D();
				transformedSource.getFixedTransform( previousTransform );
			}

			if ( mode.equals( AdditionMode.Replace ) )
			{
				transformedSource.setFixedTransform( additionalTransform.copy() );
			}
			else if ( mode.equals( AdditionMode.Concatenate ) )
			{
				AffineTransform3D newTransform = previousTransform.copy().preConcatenate( additionalTransform.copy() );
				transformedSource.setFixedTransform( newTransform );
			}

			bdvHandle.getViewerPanel().requestRepaint();
		}
		else
		{
			IJ.error( "Cannot set the transformation of a " + sourceAndConverter.getSpimSource().getClass() );
		}
	}

	public static double[] parseStringToDoubleArray(String arrayStr)
	{
		arrayStr = arrayStr.replaceAll("\\[|\\]", "");
		String[] items = arrayStr.split(",\\s*");
		double[] doubles = Arrays.stream(items).mapToDouble(Double::parseDouble).toArray();
		return doubles;
	}
}
