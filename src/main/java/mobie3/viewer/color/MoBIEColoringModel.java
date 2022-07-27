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
package mobie3.viewer.color;

import mobie3.viewer.color.opacity.OpacityAdjuster;
import mobie3.viewer.select.SelectionModel;
import net.imglib2.type.numeric.ARGBType;

import static net.imglib2.type.numeric.ARGBType.alpha;
import static net.imglib2.type.numeric.ARGBType.blue;
import static net.imglib2.type.numeric.ARGBType.green;
import static net.imglib2.type.numeric.ARGBType.red;

public class MoBIEColoringModel< T > extends AbstractColoringModel< T >
{
	private ColoringModel< T > coloringModel;
	private SelectionModel< T > selectionModel;

	private ARGBType selectionColor;
	private double opacityNotSelected;

	// Wraps a base coloring model and combines it with a selection model,
	// such that selected elements can have special colors and opacities.
	public MoBIEColoringModel( ColoringModel< T > coloringModel, SelectionModel< T > selectionModel )
	{
		setColoringModel( coloringModel );
		this.selectionModel = selectionModel;
		init();
	}

	private void init()
	{
		this.selectionColor = null;
		this.opacityNotSelected = 0.15;
	}

	@Override
	public void convert( T value, ARGBType color )
	{
		coloringModel.convert( value, color );

		if ( selectionModel == null ) return;

		if ( selectionModel.isEmpty() ) return;

		if ( ! selectionModel.isSelected( value ) )
		{
			applyOpacity( color, opacityNotSelected );
		}
		else
		{
			if ( selectionColor != null ) color.set( selectionColor );
		}
	}

	private void applyOpacity( ARGBType color, double opacity )
	{
		final int value = color.get();
		color.set( ARGBType.rgba( red( value ), green( value ), blue( value ), alpha( value ) * opacity ) );
	}

	public void setSelectionColor( ARGBType selectionColor )
	{
		this.selectionColor = selectionColor;
		notifyColoringListeners();
	}

	public void setColoringModel( ColoringModel< T > coloringModel )
	{
		this.coloringModel = coloringModel;
		notifyListeners();
	}

	private void notifyListeners()
	{
		notifyColoringListeners();
		coloringModel.listeners().add( () -> notifyColoringListeners() );
	}

	public ColoringModel< T > getWrappedColoringModel()
	{
		return coloringModel;
	}

	//public SelectionModel< T > getSelectionModel()
	//{
	//	return selectionModel;
	//}

	public double getOpacityNotSelected()
	{
		return opacityNotSelected;
	}

	public void setOpacityNotSelected( double opacityNotSelected )
	{
		this.opacityNotSelected = opacityNotSelected;

		notifyListeners();
	}

}
