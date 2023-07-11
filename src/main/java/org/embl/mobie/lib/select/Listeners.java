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
package org.embl.mobie.lib.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * A set of listeners of type {@code T}.
 *
 * @param <T>
 *            listener type
 */
public interface Listeners< T >
{
	/**
	 * Add a listener to this set.
	 *
	 * @param listener
	 *            the listener to add.
	 * @return {@code true} if the listener was added. {@code false} if it was
	 *         already present.
	 */
	boolean add( final T listener );

	/**
	 * Removes a listener from this set.
	 *
	 * @param listener
	 *            the listener to remove.
	 * @return {@code true} if the listener was successfully removed.
	 *         {@code false} if the listener was not present.
	 */
	boolean remove( final T listener );

	default boolean addAll( final Collection< ? extends T > listeners )
	{
		if ( listeners.isEmpty() )
			return false;
		return listeners.stream().map( this::add ).reduce( Boolean::logicalOr ).get();
	}

	default boolean removeAll( final Collection< ? extends T > listeners )
	{
		if ( listeners.isEmpty() )
			return false;
		return listeners.stream().map( this::remove ).reduce( Boolean::logicalOr ).get();
	}

	/**
	 * Implements {@link Listeners} using an {@link ArrayList}.
	 */
	class List< T > implements Listeners< T >
	{
		private final Consumer< T > onAdd;

		public List( final Consumer< T > onAdd )
		{
			this.onAdd = onAdd;
		}

		public List()
		{
			this( o -> {} );
		}

		public final ArrayList< T > list = new ArrayList<>();

		@Override
		public boolean add( final T listener )
		{
			if ( !list.contains( listener ) )
			{
				list.add( listener );
				onAdd.accept( listener );
				return true;
			}
			return false;
		}

		@Override
		public boolean remove( final T listener )
		{
			return list.remove( listener );
		}

		public ArrayList< T > listCopy()
		{
			return new ArrayList<>( list );
		}
	}

	/**
	 * Extends {@link List}, making {@code add} and {@code remove}
	 * methods synchronized.
	 */
	class SynchronizedList< T > extends List< T >
	{
		public SynchronizedList( final Consumer< T > onAdd )
		{
			super( onAdd );
		}

		public SynchronizedList()
		{
			super();
		}

		@Override
		public synchronized boolean add( final T listener )
		{
			return super.add( listener );
		}

		@Override
		public synchronized boolean remove( final T listener )
		{
			return super.remove( listener );
		}

		@Override
		public synchronized ArrayList< T > listCopy()
		{
			return super.listCopy();
		}
	}
}

