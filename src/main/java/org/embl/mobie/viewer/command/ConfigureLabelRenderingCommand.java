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
package org.embl.mobie.viewer.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.color.SelectionColoringModel;
import org.embl.mobie.viewer.color.SelectionColoringModelWrapper;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Label Rendering")
public class ConfigureLabelRenderingCommand implements BdvPlaygroundActionCommand
{
	public static final String SEGMENT_COLOR = "Keep current color";
	public static final String SELECTION_COLOR = "Use below selection color";

	@Parameter
	public BdvHandle bdvh;

	@Parameter
	public SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Show labels as boundaries")
	public boolean showAsBoundary;

	@Parameter ( label = "Boundary thickness", style="format:#.00" )
	public float boundaryThickness = 1.0F;

	@Parameter ( label = "Selected labels coloring", choices = { SEGMENT_COLOR, SELECTION_COLOR } )
	public String coloringMode = SEGMENT_COLOR;

	@Parameter ( label = "Selection color")
	public ColorRGB selectionColor = new ColorRGB(255,255,0);

	@Parameter ( label = "Opacity of non-selected labels" )
	public double opacity = 0.15;

	@Parameter( label = "Increment random color seed [ Ctrl L ]", callback = "incrementRandomColorSeed" )
	Button button;
	private ARGBType selectionARGB;

	public static void incrementRandomColorSeed( SourceAndConverter[] sourceAndConverters )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final Converter converter = sourceAndConverter.getConverter();

			if ( converter instanceof SelectionColoringModelWrapper )
			{
				final ColoringModel coloringModel = ( ( SelectionColoringModelWrapper ) converter ).getSelectionColoringModel().getWrappedColoringModel();

				if ( coloringModel instanceof CategoryColoringModel )
				{
					final CategoryColoringModel< ? > categoryColoringModel = ( CategoryColoringModel< ? > ) coloringModel;
					int randomSeed = categoryColoringModel.getRandomSeed();
					categoryColoringModel.setRandomSeed( ++randomSeed );
				}
			}
		}
	}

	@Override
	public void run()
	{
		configureBoundaryRendering();

		configureSelectionColoring();

		bdvh.getViewerPanel().requestRepaint();
	}

	private void configureSelectionColoring()
	{
		selectionARGB = new ARGBType( ARGBType.rgba( selectionColor.getRed(), selectionColor.getGreen(), selectionColor.getBlue(), selectionColor.getAlpha() ) );

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof SelectionColoringModelWrapper )
			{
				final SelectionColoringModel selectionColoringModel = ( ( SelectionColoringModelWrapper ) sourceAndConverter.getConverter() ).getSelectionColoringModel();

				if ( coloringMode.equals( SEGMENT_COLOR ) )
					selectionColoringModel.setSelectionColor( null );
				else
					selectionColoringModel.setSelectionColor( selectionARGB );

				selectionColoringModel.setOpacityNotSelected( opacity );
			}
		}
	}

	private void configureBoundaryRendering()
	{
		Arrays.stream( sourceAndConverters ).filter( sac -> SourceHelper.getLabelSource( sac ) != null ).forEach( sac ->
		{
			final LabelSource< ? > labelSource = SourceHelper.getLabelSource( sac );

			labelSource.showAsBoundary( showAsBoundary, boundaryThickness );

			if ( sac.asVolatile() != null )
			{
				( ( LabelSource ) sac.asVolatile().getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
			}
		});
	}

}
