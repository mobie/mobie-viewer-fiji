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
package org.embl.mobie.lib.color;

import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdditiveColoringModel< T > extends AbstractColoringModel< T >
{
	private final ArrayList< Entry< T > > entries;

	@SafeVarargs
	public AdditiveColoringModel( ColoringModel< T >... coloringModels )
	{
		this.entries = new ArrayList<>();

		for ( ColoringModel< T > coloringModel : coloringModels )
			addColoringModel( coloringModel );
	}

	public AdditiveColoringModel( List< ColoringModel< T > > coloringModels )
	{
		this.entries = new ArrayList<>();

		for ( ColoringModel< T > coloringModel : coloringModels )
			addColoringModel( coloringModel );
	}

	public void addColoringModel( ColoringModel< T > coloringModel )
	{
		if ( coloringModel == null )
			return;

		addColoringModel( ColoringModels.getName( coloringModel ), coloringModel );
	}

	public void addColoringModel( String name, ColoringModel< T > coloringModel )
	{
		addColoringModel( name, coloringModel, true );
	}

	private void addColoringModel( String name, ColoringModel< T > coloringModel, boolean enabled )
	{
		if ( coloringModel == null )
			return;

		if ( coloringModel instanceof AdditiveColoringModel )
		{
			final AdditiveColoringModel< T > additiveColoringModel = ( AdditiveColoringModel< T > ) coloringModel;
			for ( Entry< T > entry : additiveColoringModel.getEntries() )
				addColoringModel( entry.getName(), entry.getColoringModel(), entry.isEnabled() );

			return;
		}

		entries.add( new Entry<>( name, coloringModel, enabled ) );
		coloringModel.listeners().add( () -> notifyColoringListeners() );
		notifyColoringListeners();
	}

	public List< ColoringModel< T > > getColoringModels()
	{
		final ArrayList< ColoringModel< T > > coloringModels = new ArrayList<>();
		for ( Entry< T > entry : entries )
			coloringModels.add( entry.getColoringModel() );

		return Collections.unmodifiableList( coloringModels );
	}

	public List< Entry< T > > getEntries()
	{
		return Collections.unmodifiableList( entries );
	}

	public void setEnabled( Entry< T > entry, boolean enabled )
	{
		entry.setEnabled( enabled );
		notifyColoringListeners();
	}

	public boolean containsColoringModel( String name )
	{
		for ( Entry< T > entry : entries )
			if ( entry.getName().equals( name ) )
				return true;

		return false;
	}

	@Override
	public void convert( T value, ARGBType color )
	{
		final ARGBType tmp = new ARGBType();
		int r = 0;
		int g = 0;
		int b = 0;
		int a = 0;

		for ( Entry< T > entry : entries )
		{
			if ( ! entry.isEnabled() )
				continue;

			final ColoringModel< T > coloringModel = entry.getColoringModel();
			coloringModel.convert( value, tmp );

			final int argb = tmp.get();
			final int alpha = ARGBType.alpha( argb );
			if ( alpha == 0 )
				continue;

			r = addClamped( r, ARGBType.red( argb ) );
			g = addClamped( g, ARGBType.green( argb ) );
			b = addClamped( b, ARGBType.blue( argb ) );
			a = Math.max( a, alpha );
		}

		color.set( ARGBType.rgba( r, g, b, a ) );
	}

	private int addClamped( int current, int value )
	{
		final int sum = current + value;
		return sum > 255 ? 255 : sum;
	}

	public static class Entry< T >
	{
		private final String name;
		private final ColoringModel< T > coloringModel;
		private boolean enabled;

		public Entry( String name, ColoringModel< T > coloringModel )
		{
			this( name, coloringModel, true );
		}

		public Entry( String name, ColoringModel< T > coloringModel, boolean enabled )
		{
			this.name = name;
			this.coloringModel = coloringModel;
			this.enabled = enabled;
		}

		public String getName()
		{
			return name;
		}

		public ColoringModel< T > getColoringModel()
		{
			return coloringModel;
		}

		public boolean isEnabled()
		{
			return enabled;
		}

		private void setEnabled( boolean enabled )
		{
			this.enabled = enabled;
		}
	}
}
