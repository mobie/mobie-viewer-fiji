package org.embl.mobie.viewer.command.widget;

import ij.ImagePlus;
import ij.WindowManager;
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
public class SwingImagePlusListWidget extends SwingInputWidget< ImagePlus[] > implements ImagePlusListWidget< JPanel > {

	@Override
	protected void doRefresh() {
	}

	@Override
	public boolean supports( final WidgetModel model ) {
		return super.supports( model ) && model.isType( ImagePlus[].class );
	}

	@Override
	public ImagePlus[] getValue() {
		return getSelectedImagePlus();
	}

	JList< String > list;

	public ImagePlus[] getSelectedImagePlus() {
		List< String > selected = list.getSelectedValuesList();
		final ImagePlus[] imagePluses = selected.stream().map( title -> WindowManager.getImage( title ) ).collect( Collectors.toList() ).toArray( new ImagePlus[ selected.size() ] );
		return imagePluses;
	}

	@Override
	public void set(final WidgetModel model) {
		super.set(model);

		final String[] imageTitles = WindowManager.getImageTitles();
		list = new JList( imageTitles );
		list.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize( new Dimension(250, 80) );
		list.addListSelectionListener( (e)-> model.setValue(getValue()) );
		getComponent().add( listScroller );
	}
}
