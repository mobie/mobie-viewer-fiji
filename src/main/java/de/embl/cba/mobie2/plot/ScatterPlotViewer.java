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
package de.embl.cba.mobie2.plot;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.Prefs;
import bdv.viewer.TimePointListener;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.mobie.Constants;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.plot.RealPointARGBTypeBiConsumerSupplier;
import de.embl.cba.tables.plot.ScatterPlotDialog;
import de.embl.cba.tables.plot.TableRowKDTreeSupplier;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
import ij.IJ;
import net.imglib2.FinalInterval;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ScatterPlotViewer< T extends TableRow > implements SelectionListener< T >, ColoringListener, TimePointListener
{
	private final List< T > tableRows;
	private final ColoringModel< T > coloringModel;
	private final SelectionModel< T > selectionModel;

	private String[] selectedColumns;
	private double[] scaleFactors;
	private double dotSizeScaleFactor;
	private BdvHandle bdvHandle;
	private Map< T, RealPoint > tableRowToRealPoint;
	private T recentFocus;
	private Window window;
	private NearestNeighborSearchOnKDTree< T > search;
	private BdvStackSource< ARGBType > scatterPlotSource;
	private int currentTimepoint;

	public ScatterPlotViewer(
			List< T > tableRows,
			SelectionModel< T > selectionModel,
			ColoringModel< T > coloringModel,
			String[] selectedColumns,
			double[] scaleFactors,
			double dotSizeScaleFactor )
	{
		this.tableRows = tableRows;
		this.coloringModel = coloringModel;
		this.selectionModel = selectionModel;
		this.selectedColumns = selectedColumns;
		this.scaleFactors = scaleFactors;
		this.dotSizeScaleFactor = dotSizeScaleFactor;
		this.currentTimepoint = 0;
	}

	public void show()
	{
		updateScatterPlotSource();
		installBdvBehaviours();
		configureWindow();
	}

	private void updateScatterPlotSource( )
	{
		List< T > tableRows = getTableRows( );

		TableRowKDTreeSupplier< T > kdTreeSupplier = new TableRowKDTreeSupplier<>( tableRows, selectedColumns, scaleFactors );
		KDTree< T > kdTree = kdTreeSupplier.get();
		double[] min = kdTreeSupplier.getMin();
		double[] max = kdTreeSupplier.getMax();
		tableRowToRealPoint = kdTreeSupplier.getTableRowToRealPoint();
		search = new NearestNeighborSearchOnKDTree<>( kdTree );

		double aspectRatio = ( max[ 1 ] - min[ 1 ] ) / ( max[ 0 ] - min[ 0 ] );
		if ( aspectRatio > 10 || aspectRatio < 0.1 )
		{
			IJ.showMessage( "The aspect ratio, (yMax-yMin)/(xMax-xMin), of your data is " + aspectRatio + "." +
					"\nIn order to see anything you may have to scale either the x or y values" +
					"\nsuch that this ratio becomes closer to one." +
					"\nYou can do so by right-clicking into the scatter plot" +
					"\nand selecting \"Reconfigure...\"" );
		}

		Supplier< BiConsumer< RealPoint, ARGBType > > biConsumerSupplier = new RealPointARGBTypeBiConsumerSupplier<>( kdTree, coloringModel, dotSizeScaleFactor * ( min[ 0 ] - max[ 0 ] ) / 100.0, ARGBType.rgba( 100,  100, 100, 255 ) );

		FunctionRealRandomAccessible< ARGBType > randomAccessible = new FunctionRealRandomAccessible( 2, biConsumerSupplier, ARGBType::new );

		showInBdv( randomAccessible, FinalInterval.createMinMax( ( long ) min[ 0 ], ( long ) min[ 1 ], 0, ( long ) Math.ceil( max[ 0 ] ), ( long ) Math.ceil( max[ 1 ] ), 0 ), selectedColumns );
	}

	private List< T > getTableRows( )
	{
		if ( tableRows.get( 0 ).getColumnNames().contains( Constants.TIMEPOINT  ) )
		{
			return tableRows.stream().filter( t -> Double.parseDouble( t.getCell( Constants.TIMEPOINT ) ) == currentTimepoint ).collect( Collectors.toList() );
		}
		else
		{
			return tableRows;
		}
	}

	private void configureWindow()
	{
		window = SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
		window.addWindowListener(
			new WindowAdapter() {
				public void windowClosing( WindowEvent ev) {
					SwingUtilities.invokeLater( () -> window.setVisible( false ) );
				}
			});
	}

	private void installBdvBehaviours( )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), getBehavioursName() );
		behaviours.getBehaviourMap().clear();
		BdvPopupMenus.addAction( bdvHandle,"Focus closest point [Left-Click ]",
				( x, y ) -> focusAndSelectClosestPoint( search, true )
		);

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> focusAndSelectClosestPoint( search, true ), "Focus closest point", "button1" ) ;

		BdvPopupMenus.addAction( bdvHandle,"Select closest point [ Ctrl Left-Click ]",
				( x, y ) -> focusAndSelectClosestPoint( search, false )
		);

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> focusAndSelectClosestPoint( search, false ), "Select closest point", "ctrl button1" ) ;

		BdvPopupMenus.addAction( bdvHandle,"Reconfigure...",
			( x, y ) -> {
				SwingUtilities.invokeLater( () ->  {

					ScatterPlotDialog dialog = new ScatterPlotDialog( tableRows.get( 0 ).getColumnNames().stream().toArray( String[]::new ), selectedColumns, scaleFactors, dotSizeScaleFactor );

					if ( dialog.show() )
					{
						selectedColumns = dialog.getSelectedColumns();
						scaleFactors = dialog.getScaleFactors();
						dotSizeScaleFactor = dialog.getDotSizeScaleFactor();
						updateScatterPlot();
					}
				});
			}
		);
	}

	private void updateScatterPlot()
	{
		scatterPlotSource.removeFromBdv();
		updateScatterPlotSource();
	}

	private String getBehavioursName()
	{
		return "scatterplot" + selectedColumns[ 0 ] + selectedColumns[ 1 ];
	}

	private synchronized void focusAndSelectClosestPoint( NearestNeighborSearchOnKDTree< T > search, boolean focusOnly )
	{
		final T selection = searchClosestPoint( search );

		if ( selection != null )
		{
			if ( focusOnly )
			{
				recentFocus = selection;
				selectionModel.focus( selection );
			}
			else
			{
				selectionModel.toggle( selection );
				if ( selectionModel.isSelected( selection ) )
				{
					recentFocus = selection;
					selectionModel.focus( selection );
				}
			}
		}
		else
			throw new RuntimeException( "No closest point found." );
	}

	private T searchClosestPoint( NearestNeighborSearchOnKDTree< T > search )
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		RealPoint realPoint2d = new RealPoint( realPoint.getDoublePosition( 0 ), realPoint.getDoublePosition( 1 ) );
		search.search( realPoint2d );
		return search.getSampler().get();
	}

	private void showInBdv( FunctionRealRandomAccessible< ARGBType > randomAccessible, FinalInterval interval, String[] selectedColumns )
	{
		Prefs.showMultibox( false );
		Prefs.showScaleBar( false );

		final BdvOptions bdvOptions = BdvOptions.options().is2D().frameTitle( createPlotName( selectedColumns ) ).addTo( bdvHandle );

		scatterPlotSource = BdvFunctions.show(
				randomAccessible,
				interval,
				createPlotName( selectedColumns ),
				bdvOptions );

		bdvHandle = scatterPlotSource.getBdvHandle();
	}

	private static String createPlotName( String[] selectedColumns )
	{
		return "x: " + selectedColumns[ 0 ] + ", y: " + selectedColumns[ 1 ];
	}

	public String[] getSelectedColumns()
	{
		return selectedColumns;
	}

	@Override
	public void timePointChanged( int timepoint )
	{
		this.currentTimepoint = timepoint;
		updateScatterPlot();
	}

	@Override
	public void coloringChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public void selectionChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public void focusEvent( T selection )
	{
		if ( selection.getColumnNames().contains( Constants.TIMEPOINT  ) )
		{
			int selectedTimepoint = (int) Double.parseDouble( selection.getCell( Constants.TIMEPOINT ) );
			if ( selectedTimepoint != currentTimepoint )
			{
				currentTimepoint = selectedTimepoint;
				updateScatterPlotSource();
			}
		}

		if ( selection == recentFocus )
		{
			return;
		}
		else
		{
			recentFocus = selection;
			double[] location = new double[ 3 ];
			tableRowToRealPoint.get( selection ).localize( location );
			BdvUtils.moveToPosition( bdvHandle, location, 0, 500 );
		}
	}

	public Window getWindow()
	{
		return window;
	}
}
