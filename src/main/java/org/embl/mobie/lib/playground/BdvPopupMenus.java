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
package org.embl.mobie.lib.playground;

import bdv.util.BdvHandle;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class BdvPopupMenus
{
	private static ConcurrentHashMap< BdvHandle, PopupMenu > bdvToPopup = new ConcurrentHashMap<>();

	public static synchronized void addAction( BdvHandle bdvHandle, List< String > menuNames, String actionName, ClickBehaviour clickBehaviour )
	{
		ensurePopupMenuExist( bdvHandle );
		bdvToPopup.get( bdvHandle ).addPopupAction( menuNames, actionName, clickBehaviour );
	}

	public static synchronized void addAction( BdvHandle bdvHandle, String actionName, ClickBehaviour clickBehaviour )
	{
		ensurePopupMenuExist( bdvHandle );
		bdvToPopup.get( bdvHandle ).addPopupAction( actionName, clickBehaviour );
	}

	public static synchronized void removeAction( BdvHandle bdvHandle, String actionName )
	{
		bdvToPopup.get( bdvHandle ).removePopupAction( actionName );
	}

	public static synchronized void removeAllActions( BdvHandle bdvHandle )
	{
		final PopupMenu popupMenu = bdvToPopup.get( bdvHandle );
		for ( String actionName : popupMenu.getActionNames() )
		{
			removeAction( bdvHandle, actionName );
		}
	}

	public static synchronized void addAction( BdvHandle bdvHandle, String actionName, Runnable runnable )
	{
		ensurePopupMenuExist( bdvHandle );
		bdvToPopup.get( bdvHandle ).addPopupAction( actionName, runnable );
	}

	public static synchronized void removePopupMenu( BdvHandle bdvHandle )
	{
		bdvToPopup.remove( bdvHandle );
	}

	private static void ensurePopupMenuExist( BdvHandle bdvHandle )
	{
		if ( ! bdvToPopup.containsKey( bdvHandle ) )
		{
			bdvToPopup.put( bdvHandle, createPopupMenu( bdvHandle ) );
		}
	}

	private static PopupMenu createPopupMenu( BdvHandle bdvHandle )
	{
		final PopupMenu popupMenu = new PopupMenu();
		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), "popup menu" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> popupMenu.show( bdvHandle.getViewerPanel().getDisplay(), x, y ), "show popup menu", "button3", "shift P" ) ;
		return popupMenu;
	}

	public static String getCombinedMenuActionName( List< String > menuNames, String actionName )
	{
		return getCombinedMenuName( menuNames, menuNames.size() ) + " > " + actionName;
	}

	public static String getCombinedMenuName( List< String > menuNames, int depth )
	{
		return menuNames.stream().limit( depth + 1 ).collect( Collectors.joining( " > ") );
	}
}
