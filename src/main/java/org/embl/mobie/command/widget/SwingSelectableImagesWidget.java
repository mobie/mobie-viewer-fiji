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
package org.embl.mobie.command.widget;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.jetbrains.annotations.NotNull;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = InputWidget.class, priority = Priority.EXTREMELY_HIGH)
public class SwingSelectableImagesWidget extends SwingInputWidget< SelectableImages > implements SelectableImagesWidget< JPanel >
{

	JList< String > list;

	@Override
	protected void doRefresh() {
	}

	@Override
	public boolean supports( final WidgetModel model ) {
		return super.supports( model ) && model.isType( SelectableImages.class );
	}

	@Override
	public SelectableImages getValue() {
		List< String > selected = list.getSelectedValuesList();
		return new SelectableImages( selected );
	}

	@Override
	public void set(final WidgetModel model) {
		super.set(model);
		SelectableImages selectableImages = getSelectableImages( );

		list = new JList( selectableImages.getNames().toArray( new String[ 0 ] ) );
		list.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize( new Dimension(250, 80) );
		list.addListSelectionListener( (e)-> model.setValue(getValue()) );
		getComponent().add( listScroller );
	}

	@NotNull
	public static SelectableImages getSelectableImages( )
	{
		BdvHandle bdvHandle = MoBIE.getInstance().getViewManager().getSliceViewer().getBdvHandle();
		List< SourceAndConverter< ? > > sacs = MoBIEHelper.getSacs( bdvHandle ); //  MoBIEHelper.getVisibleSacs( bdvHandle );
		List< String > imageNames = sacs.stream()
				.map( sac -> sac.getSpimSource().getName() )
				.collect( Collectors.toList() );
		return new SelectableImages( imageNames );
	}
}
