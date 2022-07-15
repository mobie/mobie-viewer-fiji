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
package mobie3.viewer.plot;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.Prefs;
import bdv.viewer.TimePointListener;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.plot.RealPointARGBTypeBiConsumerSupplier;
import de.embl.cba.tables.plot.ScatterPlotDialog;
import ij.IJ;
import ij.gui.GenericDialog;
import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.table.AnnotationTableModel;
import mobie3.viewer.table.ColumnNames;
import mobie3.viewer.VisibilityListener;
import mobie3.viewer.select.SelectionListener;
import mobie3.viewer.select.SelectionModel;
import mobie3.viewer.transform.SliceViewLocationChanger;
import net.imglib2.FinalInterval;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ScatterPlotView< A extends Annotation > implements SelectionListener< A >, ColoringListener, TimePointListener
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public enum PointSelectionModes
	{
		Closest,
		WithinRadius;
	}
	private PointSelectionModes pointSelectionMode = PointSelectionModes.Closest;
	private double selectionRadius = 1.0;

	private final AnnotationTableModel< A > tableModel;
	private final ColoringModel< A > coloringModel;
	private final SelectionModel< A > selectionModel;

	private String[] selectedColumns;
	private double[] scaleFactors;
	private double dotSizeScaleFactor;
	private BdvHandle bdvHandle;
	private Map< A, RealPoint > tableRowToRealPoint;
	private A recentFocus;
	private Window window;
	private NearestNeighborSearchOnKDTree< A > nearestNeighborSearchOnKDTree;
	private BdvStackSource< ARGBType > scatterPlotSource;
	private int currentTimepoint;
	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private boolean showColumnSelectionUI = true;
	private RadiusNeighborSearchOnKDTree< A > radiusNeighborSearchOnKDTree;

	public ScatterPlotView(
			AnnotationTableModel< A > tableModel,
			SelectionModel< A > selectionModel,
			ColoringModel< A > coloringModel,
			String[] selectedColumns,
			double[] scaleFactors,
			double dotSizeScaleFactor )
	{
		this.tableModel = tableModel;
		this.coloringModel = coloringModel;
		this.selectionModel = selectionModel;
		this.selectedColumns = selectedColumns;
		this.scaleFactors = scaleFactors;
		this.dotSizeScaleFactor = dotSizeScaleFactor;
		this.currentTimepoint = 0;
	}

	public void show()
	{
		if ( window == null )
		{
			if ( showColumnSelectionUI )
			{
				ScatterPlotDialog dialog = new ScatterPlotDialog( tableModel.columnNames().toArray( new String[0] ), getSelectedColumns(), scaleFactors, dotSizeScaleFactor );

				if ( dialog.show() )
				{
					selectedColumns = dialog.getSelectedColumns();
					scaleFactors = dialog.getScaleFactors();
					dotSizeScaleFactor = dialog.getDotSizeScaleFactor();
				}
			}

			showColumnSelectionUI = false; // only show the first time

			updateScatterPlotSource();
			installBdvBehaviours();
			configureWindow();
		}
		else
		{
			window.setVisible( true );
		}
	}

	public void setShowColumnSelectionUI( boolean showColumnSelectionUI ) {
		this.showColumnSelectionUI = showColumnSelectionUI;
	}

	public List< VisibilityListener > getListeners()
	{
		return listeners;
	}

	private void updateScatterPlotSource( )
	{
		Collection< A > annotations = getAnnotationsForCurrentTimePoint( );
		AnnotationKDTreeSupplier< A > kdTreeSupplier = new AnnotationKDTreeSupplier<>( annotations, selectedColumns, scaleFactors );
		KDTree< A > kdTree = kdTreeSupplier.get();
		double[] min = kdTreeSupplier.getMin();
		double[] max = kdTreeSupplier.getMax();
		tableRowToRealPoint = kdTreeSupplier.getTableRowToRealPoint();
		nearestNeighborSearchOnKDTree = new NearestNeighborSearchOnKDTree<>( kdTree );
		radiusNeighborSearchOnKDTree = new RadiusNeighborSearchOnKDTree<>( kdTree );

		double aspectRatio = ( max[ 1 ] - min[ 1 ] ) / ( max[ 0 ] - min[ 0 ] );
		if ( aspectRatio > 10 || aspectRatio < 0.1 )
		{
			IJ.showMessage( "The aspect ratio, (yMax-yMin)/(xMax-xMin), of your data is " + aspectRatio + "." +
					"\nIn order to see something you may have to scale either the x or y values such that the aspect ratio is closer to 1.0." +
					"\nYou can change the axis scaling by right-clicking into the scatter plot and selecting \"Reconfigure...\"." );
		}

		if ( Math.abs( max[ 1 ] - min[ 1 ] ) < 1 || Math.abs( max[ 0 ] - min[ 0 ] ) < 1  )
		{
			IJ.showMessage( "The difference between the minimum and maximum value along on of the dimensions is smaller than 1.0.\n" +
					"The plot may thus appear very small.\n" +
					"You may either have to zoom in or change the axis scaling to produce larger values." +
					"\nYou can change the axis scaling by right-clicking into the scatter plot and selecting \"Reconfigure...\"." );
		}

		Supplier< BiConsumer< RealPoint, ARGBType > > biConsumerSupplier = new RealPointARGBTypeBiConsumerSupplier<>( kdTree, coloringModel, dotSizeScaleFactor * ( min[ 0 ] - max[ 0 ] ) / 100.0, ARGBType.rgba( 100,  100, 100, 255 ) );

		FunctionRealRandomAccessible< ARGBType > randomAccessible = new FunctionRealRandomAccessible( 2, biConsumerSupplier, ARGBType::new );

		showInBdv( randomAccessible, FinalInterval.createMinMax( ( long ) min[ 0 ], ( long ) min[ 1 ], 0, ( long ) Math.ceil( max[ 0 ] ), ( long ) Math.ceil( max[ 1 ] ), 0 ), selectedColumns );
	}

	private Collection< A > getAnnotationsForCurrentTimePoint( )
	{
		if ( tableModel.columnNames().contains( ColumnNames.TIMEPOINT ) )
		{
			return tableModel.rows().stream().filter( a -> (int) a.getValue( ColumnNames.TIMEPOINT ) == currentTimepoint ).collect( Collectors.toList() );
		}
		else
		{
			return tableModel.rows();
		}
	}

	private void configureWindow()
	{
		window = SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
		window.addWindowListener(
			new WindowAdapter() {
				public void windowClosing( WindowEvent ev) {
					for ( VisibilityListener listener : listeners )
					{
						BdvPopupMenus.removePopupMenu( bdvHandle );
						listener.visibility( false );
						// The window needs to be recreated because
						// BDV seems to close things...
						window = null;
						bdvHandle = null;
					}
				}
			});
	}

	private void installBdvBehaviours( )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), getBehavioursName() );
		behaviours.getBehaviourMap().clear();

		installFocusClosestPoint( behaviours );

		installSelectClosestPoints( behaviours );

		BdvPopupMenus.addAction( bdvHandle,"Reconfigure...",
			( x, y ) -> {
				SwingUtilities.invokeLater( () ->  {

					ScatterPlotDialog dialog = new ScatterPlotDialog( tableModel.columnNames().toArray( new String[ 0 ] ), getSelectedColumns(), scaleFactors, dotSizeScaleFactor );

					if ( dialog.show() )
					{
						selectedColumns = dialog.getSelectedColumns();
						scaleFactors = dialog.getScaleFactors();
						dotSizeScaleFactor = dialog.getDotSizeScaleFactor();
						if ( scatterPlotSource != null)
							scatterPlotSource.removeFromBdv();
						updateScatterPlotSource();
					}
				});
			}
		);
	}

	private void installSelectClosestPoints( Behaviours behaviours )
	{
		BdvPopupMenus.addAction( bdvHandle,"Select closest point(s) [ Ctrl Left-Click ]",
				( x, y ) -> focusAndSelectClosestPoints()
		);

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> focusAndSelectClosestPoints(), "Select closest point(s)", "ctrl button1" ) ;

		BdvPopupMenus.addAction( bdvHandle,"Configure point(s) selection...",
				( x, y ) -> {
					final GenericDialog genericDialog = new GenericDialog( "Point selection configuration" );
					genericDialog.addChoice( "Selection mode",  Arrays.stream( PointSelectionModes.values() ).map( Enum::name ).toArray( String[]::new ), pointSelectionMode.toString()  );
					genericDialog.addNumericField( "Radius", selectionRadius );
					genericDialog.showDialog();
					if ( genericDialog.wasCanceled() ) return;
					pointSelectionMode = PointSelectionModes.valueOf( genericDialog.getNextChoice() );
					selectionRadius = genericDialog.getNextNumber();
				}
		);
	}

	private void installFocusClosestPoint( Behaviours behaviours )
	{
		BdvPopupMenus.addAction( bdvHandle,"Focus closest point [ Left-Click ]",
				( x, y ) -> focusClosestPoint()
		);

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> focusClosestPoint(), "Focus closest point", "button1" ) ;
	}

	private String getBehavioursName()
	{
		return "scatterplot" + selectedColumns[ 0 ] + selectedColumns[ 1 ];
	}

	private synchronized void focusAndSelectClosestPoints( )
	{
		if ( pointSelectionMode.equals( PointSelectionModes.Closest ) )
		{
			final A selection = searchClosestPoint();

			if ( selection != null )
			{
				selectionModel.toggle( selection );
				if ( selectionModel.isSelected( selection ) )
				{
					recentFocus = selection;
					selectionModel.focus( selection, this );
				}
			}
		}
		else if ( pointSelectionMode.equals( PointSelectionModes.WithinRadius ) )
		{
			final ArrayList< A > selection = searchWithinRadius();

			if ( selection != null )
			{
				selectionModel.setSelected( selection, true );
			}
		}
	}

	private synchronized void focusClosestPoint()
	{
		final A selection = searchClosestPoint();

		if ( selection != null )
		{
			recentFocus = selection;
			selectionModel.focus( selection, this );
		}
		else
		{
			throw new RuntimeException( "No closest point found." );
		}
	}

	private A searchClosestPoint(  )
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		RealPoint realPoint2d = new RealPoint( realPoint.getDoublePosition( 0 ), realPoint.getDoublePosition( 1 ) );
		nearestNeighborSearchOnKDTree.search( realPoint2d );
		return nearestNeighborSearchOnKDTree.getSampler().get();
	}

	private ArrayList< A > searchWithinRadius(  )
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		RealPoint realPoint2d = new RealPoint( realPoint.getDoublePosition( 0 ), realPoint.getDoublePosition( 1 ) );
		radiusNeighborSearchOnKDTree.search( realPoint2d, selectionRadius, true );
		final int numNeighbors = radiusNeighborSearchOnKDTree.numNeighbors();
		final ArrayList< A > neighbors = new ArrayList<>();
		for ( int i = 0; i < numNeighbors; i++ )
		{
			neighbors.add( radiusNeighborSearchOnKDTree.getSampler( i ).get() );
		}
		return neighbors;
	}

	private void showInBdv( FunctionRealRandomAccessible< ARGBType > randomAccessible, FinalInterval interval, String[] selectedColumns )
	{
		Prefs.showMultibox( false );
		Prefs.showScaleBar( true ); // This clashes with the main BDV...

		final BdvOptions bdvOptions = BdvOptions.options().is2D().frameTitle( "Scatter plot" ).addTo( bdvHandle );

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
		if ( selectedColumns == null )
		{
			selectedColumns = new String[]{ tableModel.columnNames().get( 0 ), tableModel.columnNames().get( 1 ) };
		}
		return selectedColumns;
	}

	public boolean isVisible() { return (window != null) && window.isVisible(); }

	@Override
	public void timePointChanged( int timepoint )
	{
		this.currentTimepoint = timepoint;
		if ( window == null )
			return;
		if ( scatterPlotSource != null)
			scatterPlotSource.removeFromBdv();
		updateScatterPlotSource();
	}

	@Override
	public void coloringChanged()
	{
		if ( bdvHandle == null ) return;

		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public void selectionChanged()
	{
		if ( bdvHandle == null ) return;

		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public void focusEvent( A selection, Object initiator )
	{
		if ( bdvHandle == null ) return;

		if ( tableModel.columnNames().contains( ColumnNames.TIMEPOINT  ) )
		{
			int selectedTimePoint = (int) selection.getValue( ColumnNames.TIMEPOINT );
			if ( selectedTimePoint != currentTimepoint )
			{
				currentTimepoint = selectedTimePoint;
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
			BdvUtils.moveToPosition( bdvHandle, location, 0, SliceViewLocationChanger.animationDurationMillis );
		}
	}

	public Window getWindow()
	{
		return window;
	}

	public void hide()
	{
		if ( window != null )
			window.setVisible( false );
	}

	public void close()
	{
		if ( bdvHandle != null )
		{
			BdvPopupMenus.removePopupMenu( bdvHandle );
			bdvHandle.close();
			window = null;
		}
	}
}
