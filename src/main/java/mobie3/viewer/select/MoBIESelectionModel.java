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
package mobie3.viewer.select;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MoBIESelectionModel< T > implements SelectionModel< T >
{
	private final Listeners.SynchronizedList< SelectionListener > listeners;
	private final Set< T > selected;
	private T focusObject;

	public MoBIESelectionModel()
	{
		listeners = new Listeners.SynchronizedList<>(  );
		selected = new HashSet();
	}

	@Override
	public synchronized boolean isSelected( T object )
	{
		return selected.contains( object );
	}

	@Override
	public synchronized void setSelected( T object, boolean select )
	{
		setSelected( object, select, true );
	}

	private synchronized void setSelected( T object, boolean select, boolean notify )
	{
		if ( select )
			add( object, notify );
		else
			remove( object, notify );
	}

	private synchronized void remove( T object, boolean notify )
	{
		if ( selected.contains( object ) )
		{
			selected.remove( object );
			if ( notify )
				notifySelectionListeners();
			notifySelectionListeners();
		}
	}

	private synchronized void add( T object, boolean notify )
	{
		if ( ! selected.contains( object ) )
		{
			selected.add( object );
			if ( notify )
				notifySelectionListeners();
		}
	}

	private void notifySelectionListeners()
	{
		for ( SelectionListener listener : listeners.list )
			new Thread( () -> listener.selectionChanged() ).start();
	}

	@Override
	public synchronized void toggle( T object )
	{
		if ( selected.contains( object ) )
			remove( object, true );
		else
			add( object, true );
	}

	@Override
	public synchronized void focus( T object, Object initiator )
	{
		focusObject = object;

		for ( SelectionListener listener : listeners.list )
			new Thread( () -> listener.focusEvent( object, initiator ) ).start();
	}

	@Override
	public boolean isFocused( T object )
	{
		if ( focusObject != null && focusObject.equals( object ) )
			return true;
		else
			return false;
	}

	@Override
	public synchronized boolean setSelected( Collection< T > objects, boolean select )
	{
		for( T object : objects )
			setSelected( object, select, false );

		notifySelectionListeners();

		return true;
	}

	@Override
	public synchronized boolean clearSelection()
	{
		if ( selected.size() == 0 )
			return false;
		else
		{
			selected.clear();
			notifySelectionListeners();
			return true;
		}
	}

	@Override
	public synchronized Set< T > getSelected()
	{
		return new HashSet< T >( selected );
	}

	@Override
	public boolean isEmpty()
	{
		return selected.isEmpty();
	}

	@Override
	public Listeners< SelectionListener > listeners()
	{
		return listeners;
	}

	@Override
	public void resumeListeners()
	{

	}

	@Override
	public void pauseListeners()
	{

	}

}
