/*-
 * #%L
 * Various Java code for ImageJ
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

import org.scijava.ui.behaviour.ClickBehaviour;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class PopupMenu
{
	private JPopupMenu popup;
	private int x;
	private int y;
	private Map< String, JMenuItem > actionNameToMenuItem;
	private Map< String, JMenu > menuNameToMenu;
	private Set< JMenu > menus;

	public PopupMenu()
	{
		createPopupMenu();
	}

	private void createPopupMenu()
	{
		actionNameToMenuItem = new HashMap<>(  );
		menuNameToMenu = new HashMap<>(  );
		menus = new HashSet<>(  );
		popup = new JPopupMenu();
	}

	private void addPopupLine() {
		popup.addSeparator();
	}

	public void addPopupAction( String actionName, ClickBehaviour clickBehaviour )
	{
		if ( actionNameToMenuItem.keySet().contains( actionName ) )
			throw new UnsupportedOperationException( actionName + " is already registered in this popup menu." );

		JMenuItem menuItem = new JMenuItem( actionName );
		menuItem.addActionListener( e -> new Thread( () -> clickBehaviour.click( x, y ) ).start() );
		popup.add( menuItem );
		actionNameToMenuItem.put( actionName, menuItem );
	}

	public void addPopupAction( List< String > menuNames, String actionName, ClickBehaviour clickBehaviour )
	{
		final String menuActionName = BdvPopupMenus.getCombinedMenuActionName( menuNames, actionName );

		if ( actionNameToMenuItem.keySet().contains( menuActionName ) )
			throw new UnsupportedOperationException( menuActionName + " is already registered in this popup menu." );

		JMenu menu = getMenu( menuNames, 0 );
		popup.add( menu );

		menus.add( menu );
		for ( int i = 1; i < menuNames.size(); i++ )
		{
			final JMenu subMenu = getMenu( menuNames, i );
			menu.add( subMenu );
			menus.add( subMenu );
			menu = subMenu;
		}

		JMenuItem menuItem = new JMenuItem( actionName );
		menu.add( menuItem );
		menuItem.addActionListener( e -> new Thread( () -> clickBehaviour.click( x, y ) ).start() );
		actionNameToMenuItem.put( menuActionName, menuItem );
	}

	private JMenu getMenu( List< String > menuNames, int i )
	{
		final String combinedMenuName = BdvPopupMenus.getCombinedMenuName( menuNames, i );

		if ( ! menuNameToMenu.containsKey( combinedMenuName ) )
		{
			menuNameToMenu.put( combinedMenuName, new JMenu( menuNames.get( i ) ) );
		}

		return menuNameToMenu.get( combinedMenuName );
	}

	public void removePopupAction( String actionName  )
	{
		if ( ! actionNameToMenuItem.keySet().contains( actionName ) ) return;
		final JMenuItem jMenuItem = actionNameToMenuItem.get( actionName );

		final Container parent = jMenuItem.getParent();
		if ( parent instanceof JPopupMenu )
		{
			parent.remove( jMenuItem );
		}

		for ( int i = 0; i < 3; i++ ) // iterate as menus could be nested
		{
			removeEmptyMenus();
		}

		actionNameToMenuItem.remove( actionName );
	}

	public Set< String > getActionNames()
	{
		return actionNameToMenuItem.keySet();
	}

	private void removeEmptyMenus()
	{
		final ArrayList< JMenu > removed = new ArrayList<>();
		for ( JMenu menu : menus )
		{
			if ( menu.getItemCount() == 0 )
			{
				menu.getParent().remove( menu );
				removed.add( menu );
			}
		}

		for ( JMenu menu : removed )
		{
			menus.remove( menu );
		}
	}

	public void addPopupAction( String actionName, Runnable runnable ) {

		JMenuItem menuItem = new JMenuItem( actionName );
		menuItem.addActionListener( e -> new Thread( () -> runnable.run() ).start() );
		popup.add( menuItem );
	}

	public void show( JComponent display, int x, int y )
	{
		this.x = x;
		this.y = y;
		popup.show( display, x, y );
	}
}
