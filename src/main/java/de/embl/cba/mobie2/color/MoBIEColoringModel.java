/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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
package de.embl.cba.mobie2.color;

import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.AbstractColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.select.SelectionModel;
import net.imglib2.type.numeric.ARGBType;

import java.util.Arrays;
import java.util.List;

public class MoBIEColoringModel< T > extends AbstractColoringModel< T >
{
	private ColoringModel< T > coloringModel;
	private SelectionModel< T > selectionModel;

	private SelectionColoringMode selectionColoringMode;
	private ARGBType selectionColor;
	private double alphaNotSelected;

	public static final ARGBType YELLOW = new ARGBType( ARGBType.rgba( 255, 255, 0, 255 ) );

	private final List< SelectionColoringMode > selectionColoringModes;

	public enum SelectionColoringMode
	{
		SelectionColor,
		SelectionColorAndDimNotSelected,
		DimNotSelected
	}

	// TODO: Could one *not* make this a wrapper but rather configurable (in terms of table column coloring)
	public MoBIEColoringModel( String lut )
	{
		setColoringModel( new LazyCategoryColoringModel<>( new LutFactory().get( lut ) ) );

		this.selectionColoringModes = Arrays.asList( SelectionColoringMode.values() );
		this.selectionColor = YELLOW;
		this.alphaNotSelected = 0.15;
		this.selectionColoringMode = SelectionColoringMode.DimNotSelected;
	}

	public void setSelectionModel( SelectionModel< T > selectionModel )
	{
		this.selectionModel = selectionModel;
	}

	@Override
	public void convert( T input, ARGBType output )
	{
		coloringModel.convert( input, output );

		if ( selectionModel == null ) return;

		// TODO: this is very inefficient as it recomputes the colors all the time
		//   implement a selectionChanged Listener!
		if ( selectionModel.isEmpty() ) return;

		final boolean isSelected = selectionModel.isSelected( input );

		switch ( selectionColoringMode )
		{
			case DimNotSelected:
				if ( ! isSelected )
					dim( output, alphaNotSelected );
				break;

			case SelectionColor:
				if ( isSelected )
					output.set( selectionColor );
				break;

			case SelectionColorAndDimNotSelected:
				if ( isSelected )
					output.set( selectionColor );
				else
					dim( output, alphaNotSelected );
				break;

			default:
				break;
		}
	}

	/**
	 * Implements dimming via alpha
	 *
	 * @param output
	 * @param alpha
	 */
	private void dim( ARGBType output, double alpha )
	{
		final int colorIndex = output.get();

		output.set(
				ARGBType.rgba(
						ARGBType.red( colorIndex ),
						ARGBType.green( colorIndex ),
						ARGBType.blue( colorIndex ),
						alpha * 255 )
		);
	}

	public void setSelectionColoringMode( SelectionColoringMode selectionColoringMode )
	{
		this.selectionColoringMode = selectionColoringMode;
		notifyColoringListeners();
	}

	public void setSelectionColoringMode( SelectionColoringMode selectionColoringMode, double brightnessNotSelected )
	{
		this.selectionColoringMode = selectionColoringMode;

		// ensure value between 0 and 1
		brightnessNotSelected = Math.min( 1.0, brightnessNotSelected );
		brightnessNotSelected = Math.max( 0.0, brightnessNotSelected );
		this.alphaNotSelected = brightnessNotSelected;

		notifyColoringListeners();
	}

	public SelectionColoringMode getSelectionColoringMode()
	{
		return selectionColoringMode;
	}

	public void setSelectionColor( ARGBType selectionColor )
	{
		this.selectionColor = selectionColor;
		notifyColoringListeners();
	}

	public void setColoringModel( ColoringModel< T > coloringModel )
	{
		this.coloringModel = coloringModel;
		notifyColoringListeners();

		// chain event notification
		coloringModel.listeners().add( () -> notifyColoringListeners() );
	}

	public ColoringModel< T > getColoringModel()
	{
		return coloringModel;
	}

	public void iterateSelectionMode()
	{
		final int selectionModeIndex = selectionColoringModes.indexOf( selectionColoringMode );

		if ( selectionModeIndex < selectionColoringModes.size() - 1 )
		{
			setSelectionColoringMode( selectionColoringModes.get( selectionModeIndex + 1 ) );
		}
		else
		{
			setSelectionColoringMode( selectionColoringModes.get( 0 ) );
		}
	}

	public SelectionModel< T > getSelectionModel()
	{
		return selectionModel;
	}

	public double getAlphaNotSelected()
	{
		return alphaNotSelected;
	}

}
