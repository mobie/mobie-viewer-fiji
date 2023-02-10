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
package org.embl.mobie.lib.plot;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.TimePointListener;
import ij.IJ;
import ij.gui.GenericDialog;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.playground.BdvPopupMenus;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.select.SelectionModel;
import org.embl.mobie.lib.serialize.display.VisibilityListener;
import org.embl.mobie.lib.table.AnnotationTableModel;
import org.embl.mobie.lib.transform.SliceViewLocationChanger;
import org.embl.mobie.lib.transform.TransformHelper;
import org.embl.mobie.lib.ui.ColumnColoringModelDialog;
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

	private double[] min;
	private double[] max;
	private boolean showAllTimepoints = true;

	public enum PointSelectionModes
	{
		Closest,
		WithinRadius;
	}

	private PointSelectionModes pointSelectionMode = PointSelectionModes.Closest;
	private double selectionRadius = 1.0;

	private final AnnotationTableModel< A > tableModel;
	private final MobieColoringModel< A > coloringModel;
	private final SelectionModel< A > selectionModel;

	private String[] selectedColumns;
	private double[] axesScaleFactors;
	private double dotSizeScaleFactor;
	private BdvHandle bdvHandle;
	private Map< A, RealPoint > tableRowToRealPoint;
	private A recentFocus;
	private Window window;
	private NearestNeighborSearchOnKDTree< A > nearestNeighborSearchOnKDTree;
	private BdvStackSource< ARGBType > scatterPlotSource;
	private int currentTimePoint;
	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private RadiusNeighborSearchOnKDTree< A > radiusNeighborSearchOnKDTree;

	public ScatterPlotView(
			AnnotationTableModel< A > tableModel,
			SelectionModel< A > selectionModel,
			MobieColoringModel< A > coloringModel,
			String[] selectedColumns,
			double[] axesScaleFactors,
			double dotSizeScaleFactor )
	{
		this.tableModel = tableModel;
		this.coloringModel = coloringModel;
		this.selectionModel = selectionModel;
		this.selectedColumns = selectedColumns;
		this.axesScaleFactors = axesScaleFactors;
		this.dotSizeScaleFactor = dotSizeScaleFactor;
		this.currentTimePoint = 0;
	}

	public synchronized void show( boolean showDialog )
	{
		if ( bdvHandle != null )
		{
			window.setVisible( true );
			return;
		}

		if ( showDialog )
		{
			configureViaDialog();
		}

		updatePlot();
		installBdvBehaviours();
		configureWindowClosing();
	}

	public List< VisibilityListener > getListeners()
	{
		return listeners;
	}

	private void updatePlot( )
	{
		if ( scatterPlotSource != null)
			scatterPlotSource.removeFromBdv();

		Collection< A > annotations = getAnnotationsForCurrentTimePoint( );
		AnnotationKDTreeSupplier< A > kdTreeSupplier = new AnnotationKDTreeSupplier<>( annotations, selectedColumns, axesScaleFactors );
		KDTree< A > kdTree = kdTreeSupplier.get();
		min = kdTreeSupplier.getMin();
		max = kdTreeSupplier.getMax();
		tableRowToRealPoint = kdTreeSupplier.getAnnotationToRealPoint();
		nearestNeighborSearchOnKDTree = new NearestNeighborSearchOnKDTree<>( kdTree );
		radiusNeighborSearchOnKDTree = new RadiusNeighborSearchOnKDTree<>( kdTree );

		double aspectRatio = ( max[ 1 ] - min[ 1 ] ) / ( max[ 0 ] - min[ 0 ] );
		if ( aspectRatio > 10 || aspectRatio < 0.1 )
		{
			IJ.showMessage( "The aspect ratio, (yMax-yMin)/(xMax-xMin), of your data is " + aspectRatio + "." +
					"\nThe plot may look better scaling either the x or y values such that the aspect ratio is closer to 1.0." +
					"\nYou can change the scaling by right-clicking into the scatter plot and selecting \"Reconfigure Plot...\"." );
		}

//		if ( Math.abs( max[ 1 ] - min[ 1 ] ) < 1 || Math.abs( max[ 0 ] - min[ 0 ] ) < 1  )
//		{
//			IJ.showMessage( "The difference between the minimum and maximum value along on of the dimensions is smaller than 1.0.\n" +
//					"The plot may thus appear very small.\n" +
//					"You may either have to zoom in or change the axis scaling to produce larger values." +
//					"\nYou can change the axis scaling by right-clicking into the scatter plot and selecting \"Reconfigure...\"." );
//		}

		Supplier< BiConsumer< RealPoint, ARGBType > > biConsumerSupplier = new RealPointARGBTypeBiConsumerSupplier( kdTree, coloringModel, dotSizeScaleFactor * ( min[ 0 ] - max[ 0 ] ) / 100.0, ARGBType.rgba( 100,  100, 100, 255 ) );

		FunctionRealRandomAccessible< ARGBType > realRandomAccessible = new FunctionRealRandomAccessible( 2, biConsumerSupplier, ARGBType::new );

		showInBdv( realRandomAccessible, FinalInterval.createMinMax( ( long ) min[ 0 ], ( long ) min[ 1 ], 0, ( long ) Math.ceil( max[ 0 ] ), ( long ) Math.ceil( max[ 1 ] ), 0 ) );
	}

	private Collection< A > getAnnotationsForCurrentTimePoint( )
	{
		if ( showAllTimepoints )
			return tableModel.annotations();
		else
			return tableModel.annotations().stream().filter( annotation -> annotation.timePoint() == currentTimePoint ).collect( Collectors.toList() );
	}

	private void configureWindowClosing()
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

		BdvPopupMenus.addAction( bdvHandle,"Configure Plot...",
			( x, y ) ->
			{
				SwingUtilities.invokeLater( () ->  {
					configureViaDialog();
				});
			}
		);

		BdvPopupMenus.addAction( bdvHandle, "Color by Column...",
				( x, y ) ->
				{
					final ColoringModel< A > coloringModel = new ColumnColoringModelDialog<>( tableModel  ).showDialog();

					if ( coloringModel != null )
						this.coloringModel.setColoringModel( coloringModel );
				});

		installPointSelections( behaviours );

	}

	private void configureViaDialog()
	{
		ScatterPlotDialog dialog = new ScatterPlotDialog( tableModel.columnNames().toArray( new String[ 0 ] ), getSelectedColumns(), axesScaleFactors, dotSizeScaleFactor, showAllTimepoints );

		if ( dialog.show() )
		{
			updateSettings( dialog );
			updatePlot();
		}
	}

	private void updateSettings( ScatterPlotDialog dialog )
	{
		selectedColumns = dialog.getSelectedColumns();
		axesScaleFactors = dialog.getAxesScaleFactors();
		dotSizeScaleFactor = dialog.getDotSizeScaleFactor();
		showAllTimepoints = dialog.isShowAllTimepoints();
	}

	private void installPointSelections( Behaviours behaviours )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> focusClosestPoint(), "Focus closest point", "button1" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> focusAndSelectClosestPoints(), "Select closest point(s)", "ctrl button1" ) ;

		BdvPopupMenus.addAction( bdvHandle,"Configure Point(s) Selection...",
				( x, y ) -> {
					final GenericDialog genericDialog = new GenericDialog( "Point selection configuration" );
					genericDialog.addMessage( "Focus: [ Left-Click ]" );
					genericDialog.addMessage( "Select: [ Ctrl Left-Click ]" );

					genericDialog.addChoice( "Selection mode",  Arrays.stream( PointSelectionModes.values() ).map( Enum::name ).toArray( String[]::new ), pointSelectionMode.toString()  );
					genericDialog.addNumericField( "Radius", selectionRadius );
					genericDialog.showDialog();
					if ( genericDialog.wasCanceled() ) return;
					pointSelectionMode = PointSelectionModes.valueOf( genericDialog.getNextChoice() );
					selectionRadius = genericDialog.getNextNumber();
				}
		);
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
					logCoordinates( selection );
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

	private void logCoordinates( A selection )
	{
		final Double x = selection.getNumber( selectedColumns[ 0 ] );
		final Double y = selection.getNumber( selectedColumns[ 1 ] );
		IJ.log( selection.uuid() + ": " + x + ", " + y );
	}

	private synchronized void focusClosestPoint()
	{
		final A selection = searchClosestPoint();

		if ( selection != null )
		{
			logCoordinates( selection );
			recentFocus = selection;
			selectionModel.focus( selection, this );
		}
		else
		{
			throw new RuntimeException( "No closest point found." );
		}
	}

	private A searchClosestPoint( )
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		RealPoint realPoint2d = new RealPoint( realPoint.getDoublePosition( 0 ), realPoint.getDoublePosition( 1 ) );
		nearestNeighborSearchOnKDTree.search( realPoint2d );
		return nearestNeighborSearchOnKDTree.getSampler().get();
	}

	private ArrayList< A > searchWithinRadius( )
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		RealPoint realPoint2d = new RealPoint( realPoint.getDoublePosition( 0 ), realPoint.getDoublePosition( 1 ) );
		radiusNeighborSearchOnKDTree.search( realPoint2d, selectionRadius, true );
		final int numNeighbors = radiusNeighborSearchOnKDTree.numNeighbors();
		final ArrayList< A > neighbors = new ArrayList<>();
		for ( int i = 0; i < numNeighbors; i++ )
			neighbors.add( radiusNeighborSearchOnKDTree.getSampler( i ).get() );

		return neighbors;
	}

	private void showInBdv( FunctionRealRandomAccessible< ARGBType > realRandomAccessible, FinalInterval interval )
	{
		final BdvOptions bdvOptions = BdvOptions.options().is2D().frameTitle( "Scatter plot" ).addTo( bdvHandle );

		scatterPlotSource = BdvFunctions.show(
				realRandomAccessible,
				interval,
				createPlotName( selectedColumns ),
				bdvOptions );

		bdvHandle = scatterPlotSource.getBdvHandle();

		// zoom to see all points
		final double[] min3D = new double[ 3 ];
		final double[] max3D = new double[ 3 ];
		for ( int d = 0; d < 2; d++ )
		{
			min3D[ d ] = min[ d ];
			max3D[ d ] = max[ d ];
		}
		final FinalRealInterval bounds = new FinalRealInterval( min3D, max3D );
		final AffineTransform3D transform = TransformHelper.getIntervalViewerTransform( bdvHandle, bounds );
		bdvHandle.getViewerPanel().state().setViewerTransform( transform );
	}

	private static String createPlotName( String[] selectedColumns )
	{
		return "x: " + selectedColumns[ 0 ] + ", y: " + selectedColumns[ 1 ];
	}

	public String[] getSelectedColumns()
	{
		if ( selectedColumns == null )
			selectedColumns = new String[]{ tableModel.columnNames().get( 0 ), tableModel.columnNames().get( 1 ) };

		return selectedColumns;
	}

	public boolean isVisible() { return (window != null) && window.isVisible(); }

	@Override
	public void timePointChanged( int timepoint )
	{
		this.currentTimePoint = timepoint;
		if ( showAllTimepoints ) return;
		if ( window == null )  return;
		updatePlot();
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

		int selectedTimePoint = selection.timePoint();
		if ( selectedTimePoint != currentTimePoint )
		{
			currentTimePoint = selectedTimePoint;
			if ( ! showAllTimepoints )
				updatePlot();
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
			SliceViewLocationChanger.moveToPosition( bdvHandle, location, SliceViewLocationChanger.animationDurationMillis );
		}
	}

	public Window getWindow()
	{
		return window;
	}

	public boolean isShown()
	{
		return window != null  && window.isVisible();
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
