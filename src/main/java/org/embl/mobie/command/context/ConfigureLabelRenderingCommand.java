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

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.color.CategoricalAnnotationColoringModel;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.color.MobieColoringModelWrapper;
import org.embl.mobie.lib.source.BoundarySource;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.source.VolatileBoundarySource;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Label Rendering")
public class ConfigureLabelRenderingCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Initializable
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }
	protected static final String SEGMENT_COLOR = "Keep current color";
	protected static final String SELECTION_COLOR = "Use below selection color";

	@Parameter
	protected BdvHandle bdvh;

	@Parameter
	protected SourceAndConverter< ? >[] sourceAndConverters;

	@Parameter( label = "Show labels as boundaries", persist = false )
	public boolean showAsBoundary;

	@Parameter( label = "Boundary thickness", callback = "logBoundaryThickness", style = "format:#.00", persist = false )
	public double boundaryThickness = 1.0F;

	@Parameter( label = "Label coloring", choices = { SEGMENT_COLOR, SELECTION_COLOR } )
	public String coloringMode = SEGMENT_COLOR;

	@Parameter( label = "Selection color" )
	public ColorRGB selectionColor = new ColorRGB( 255, 255, 0 );

	@Parameter( label = "Opacity of non-selected labels", style = "format:#.00" )
	public double opacity = 0.15;

	// persist = false is needed for the {@code initialise} method to work
	@Parameter( label = "Random label color seed [ Ctrl L ]", persist = false )
	public Integer randomColorSeed = 42;

	protected ARGBType selectionARGB;

	@Override
	public void initialize()
	{
		initRandomColorSeedItem();
		initBoundaryRendering();
	}

	protected void initRandomColorSeedItem()
	{
		final MutableModuleItem< Integer > randomColorSeedItem = getInfo().getMutableInput("randomColorSeed", Integer.class );

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final Converter converter = sourceAndConverter.getConverter();

			if ( converter instanceof MobieColoringModelWrapper )
			{
				final ColoringModel coloringModel = ( ( MobieColoringModelWrapper ) converter ).getMoBIEColoringModel().getWrappedColoringModel();

				if ( coloringModel instanceof CategoricalAnnotationColoringModel )
				{
					final CategoricalAnnotationColoringModel< ? > categoricalAnnotationColoringModel = ( CategoricalAnnotationColoringModel< ? > ) coloringModel;
					final int randomSeed = categoricalAnnotationColoringModel.getRandomSeed();
					randomColorSeedItem.setValue( this, randomSeed );
					return;
				}
			}
		}
	}

	protected void initBoundaryRendering()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			final BoundarySource< ? > boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );

			if ( boundarySource != null )
			{
				final MutableModuleItem< Boolean > showAsBoundaryItem = getInfo().getMutableInput( "showAsBoundary", Boolean.class );
				final boolean showAsBoundaries = boundarySource.showAsBoundaries();
				showAsBoundaryItem.setValue( this, showAsBoundaries );

				final MutableModuleItem< Double > boundaryThicknessItem = getInfo().getMutableInput( "boundaryThickness", Double.class );
				boundaryThicknessItem.setValue( this, boundarySource.getBoundaryWidth() );
				return;
			}
		}
	}


	@Override
	public void run()
	{
		configureBoundaryRendering();

		configureSelectionColoring();

		configureRandomColorSeed();

		bdvh.getViewerPanel().requestRepaint();
	}

	protected void configureRandomColorSeed()
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final Converter converter = sourceAndConverter.getConverter();

			if ( converter instanceof MobieColoringModelWrapper )
			{
				final ColoringModel coloringModel = ( ( MobieColoringModelWrapper ) converter ).getMoBIEColoringModel().getWrappedColoringModel();

				if ( coloringModel instanceof CategoricalAnnotationColoringModel )
				{
					final CategoricalAnnotationColoringModel< ? > categoricalAnnotationColoringModel = ( CategoricalAnnotationColoringModel< ? > ) coloringModel;
					categoricalAnnotationColoringModel.setRandomSeed( randomColorSeed );
				}
			}
		}
	}

	protected void configureSelectionColoring()
	{
		selectionARGB = new ARGBType( ARGBType.rgba( selectionColor.getRed(), selectionColor.getGreen(), selectionColor.getBlue(), selectionColor.getAlpha() ) );

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final Converter converter = sourceAndConverter.getConverter();

			// Here it is sufficient to do this for the non-volatile
			// converter, because the volatile converter shares the
			// same instance of the selectionColoringModel.
			if ( converter instanceof MobieColoringModelWrapper )
			{
				final MobieColoringModel moBIEColoringModel = ( ( MobieColoringModelWrapper ) converter ).getMoBIEColoringModel();

				if ( coloringMode.equals( SEGMENT_COLOR ) )
					moBIEColoringModel.setSelectionColor( null );
				else
					moBIEColoringModel.setSelectionColor( selectionARGB );

				moBIEColoringModel.setOpacityNotSelected( opacity );
			}
		}
	}

	protected void configureBoundaryRendering()
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
			if ( boundarySource != null )
				boundarySource.showAsBoundary( showAsBoundary, boundaryThickness );

			if ( sourceAndConverter.asVolatile() != null )
			{
				final VolatileBoundarySource volatileBoundarySource = SourceHelper.unwrapSource( sourceAndConverter.asVolatile().getSpimSource(), VolatileBoundarySource.class );
				if ( volatileBoundarySource != null )
					volatileBoundarySource.showAsBoundary( showAsBoundary, boundaryThickness );
			}
		}
	}

	protected void logBoundaryThickness()
	{
		final String unit = sourceAndConverters[ 0 ].getSpimSource().getVoxelDimensions().unit();
		IJ.log("Thickness: " + boundaryThickness + " " + unit );
	}

	public static void incrementRandomColorSeed( SourceAndConverter[] sourceAndConverters, BdvHandle bdvh )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final Converter converter = sourceAndConverter.getConverter();

			if ( converter instanceof MobieColoringModelWrapper )
			{
				final ColoringModel coloringModel = ( ( MobieColoringModelWrapper ) converter ).getMoBIEColoringModel().getWrappedColoringModel();

				if ( coloringModel instanceof CategoricalAnnotationColoringModel )
				{
					final CategoricalAnnotationColoringModel< ? > categoricalAnnotationColoringModel = ( CategoricalAnnotationColoringModel< ? > ) coloringModel;
					int randomSeed = categoricalAnnotationColoringModel.getRandomSeed();
					categoricalAnnotationColoringModel.setRandomSeed( ++randomSeed );
				}
			}
		}

		bdvh.getViewerPanel().requestRepaint();
	}
}
