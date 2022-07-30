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
package org.embl.mobie.viewer.volume;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import customnode.CustomTriangleMesh;
import de.embl.cba.tables.ij3d.AnimatedViewAdjuster;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;
import org.embl.mobie.viewer.VisibilityListener;
import org.embl.mobie.viewer.annotation.Segment;
import org.embl.mobie.viewer.color.ColorHelper;
import org.embl.mobie.viewer.color.ColoringListener;
import org.embl.mobie.viewer.color.ColoringModel;
import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.select.SelectionModel;
import org.embl.mobie.viewer.source.AnnotationType;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SegmentsVolumeViewer< AS extends Segment > implements ColoringListener, SelectionListener< AS >
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final SelectionModel< AS > selectionModel;
	private final ColoringModel< AS > coloringModel;
	private final Collection< SourceAndConverter< AnnotationType< TableRowImageSegment > > > sourceAndConverters;
	private final UniverseManager universeManager;

	private ConcurrentHashMap< AS, Content > segmentToContent;
	private ConcurrentHashMap< Content, AS > contentToSegment;
	private double transparency;
	private int meshSmoothingIterations;
	private int segmentFocusAnimationDurationMillis;
	private double segmentFocusZoomLevel;
	private double segmentFocusDxyMin;
	private double segmentFocusDzMin;
	private long maxNumVoxels;
	private String objectsName;
	private boolean showSegments = false;
	private double[] voxelSpacing; // desired voxel spacings; null = auto
	private int currentTimePoint = 0;
	private final MeshCreator< AS > meshCreator;
	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private Window window;
	private Image3DUniverse universe;

	public SegmentsVolumeViewer(
			final SelectionModel< AS > selectionModel,
			final ColoringModel< AS > coloringModel,
			final Collection< SourceAndConverter< AnnotationType< TableRowImageSegment > > > sourceAndConverters,
			UniverseManager universeManager )
	{
		this.selectionModel = selectionModel;
		this.coloringModel = coloringModel;
		this.sourceAndConverters = sourceAndConverters;
		this.universeManager = universeManager;

		this.transparency = 0.2;
		this.meshSmoothingIterations = 5;
		this.segmentFocusAnimationDurationMillis = 750;
		this.segmentFocusZoomLevel = 0.8;
		this.segmentFocusDxyMin = 20.0;
		this.segmentFocusDzMin = 20.0;
		this.maxNumVoxels = 100 * 100 * 100;
		this.objectsName = "";
		this.segmentToContent = new ConcurrentHashMap<>();
		this.contentToSegment = new ConcurrentHashMap<>();

		this.meshCreator = new MeshCreator<>( meshSmoothingIterations, maxNumVoxels );
	}

	public void setObjectsName( String objectsName )
	{
		if ( objectsName == null )
			throw new RuntimeException( "Cannot set objects name in Segments3dView to null." );

		this.objectsName = objectsName;
	}

	public void setTransparency( double transparency )
	{
		this.transparency = transparency;
	}

	public void setMeshSmoothingIterations( int iterations )
	{
		this.meshSmoothingIterations = iterations;
	}

	public void setSegmentFocusAnimationDurationMillis( int duration )
	{
		this.segmentFocusAnimationDurationMillis = duration;
	}

	public void setSegmentFocusZoomLevel( double segmentFocusZoomLevel )
	{
		this.segmentFocusZoomLevel = segmentFocusZoomLevel;
	}

	public void setSegmentFocusDxyMin( double segmentFocusDxyMin )
	{
		this.segmentFocusDxyMin = segmentFocusDxyMin;
	}

	public void setSegmentFocusDzMin( double segmentFocusDzMin )
	{
		this.segmentFocusDzMin = segmentFocusDzMin;
	}

	public void setMaxNumVoxels( long maxNumVoxels )
	{
		this.maxNumVoxels = maxNumVoxels;
	}

	private void updateSegmentColors()
	{
		for ( AS segment : segmentToContent.keySet() )
		{
			final Color3f color3f = getColor3f( segment );
			final Content content = segmentToContent.get( segment );
			content.setColor( color3f );
		}
	}

	public synchronized void updateView( boolean recomputeMeshes )
	{
		new Thread( () ->
		{
			universe.setAutoAdjustView( true );
			updateSelectedSegments( recomputeMeshes );
			removeUnselectedSegments();
		}).start();
	}

	private void removeUnselectedSegments( )
	{
		final Set< AS > selectedSegments = selectionModel.getSelected();
		final Set< AS > currentSegments = segmentToContent.keySet();
		final Set< AS > remove = new HashSet<>();

		for ( AS segment : currentSegments )
			if ( ! selectedSegments.contains( segment ) )
				remove.add( segment );

		for( AS segment : remove )
			removeSegment( segment );
	}

	private synchronized void updateSelectedSegments( boolean recomputeMeshes )
	{
		final Set< AS > selected = selectionModel.getSelected();

		for ( AS segment : selected )
		{
			if ( segment.timePoint() == currentTimePoint )
			{
				if ( recomputeMeshes ) removeSegment( segment );

				if ( ! segmentToContent.containsKey( segment ) )
				{
					final Source< AnnotationType< AS > > source = getSource( segment );
					final CustomTriangleMesh mesh = meshCreator.createSmoothCustomTriangleMesh( segment, voxelSpacing, recomputeMeshes, source );
					mesh.setColor( getColor3f( segment ) );
					addSegmentMeshToUniverse( segment, mesh );
				}
			}
			else // segment is of another time point
			{
				removeSegment( segment );
			}
		}
	}

	private Source< AnnotationType< AS > > getSource( AS segment )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getSpimSource().getName().equals( segment.imageId() ))
			{
				return ( Source< AnnotationType< AS > > ) sourceAndConverter.getSpimSource();
			}
		}

		throw new UnsupportedOperationException( "An image segment from " + segment.imageId() + " did not have a corresponding image source."  );
	}

	private synchronized void removeSegment( AS segment )
	{
		final Content content = segmentToContent.get( segment );
		universe.removeContent( content.getName() );
		segmentToContent.remove( segment );
		contentToSegment.remove( content );
	}

	private String getSegmentIdentifier( AS segment )
	{
		return segment.label() + "-" + segment.timePoint();
	}

	public synchronized void showSegments( boolean showSegments, boolean autoAdjustView )
	{
		if ( showSegments && universe == null )
		{
			universe = universeManager.get();
			configureUniverseListener();
			window = universe.getWindow();
			window.addWindowListener(
				new WindowAdapter()
				{
					public void windowClosing( WindowEvent ev )
					{
						window = null;
						universe = null;
						segmentToContent.clear();
						contentToSegment.clear();
						setShowSegments( false );
						universeManager.setUniverse( null );
						for ( VisibilityListener listener : listeners )
						{
							listener.visibility( false );
						}
					}
				} );
		}

		if ( showSegments != this.showSegments )
		{
			this.showSegments = showSegments;
			if ( showSegments )
			{
				universe.setAutoAdjustView( autoAdjustView );
				updateView( false );
			}
			else
			{
				new Thread( () -> removeSegments() ).start();
			}
		}
	}

	public boolean isShowSegments() {
		return showSegments;
	}

	private void setShowSegments( boolean b )
	{
		this.showSegments = b;
	}

	private void removeSegments()
	{
		final Set< AS > segments = selectionModel.getSelected();

		for ( AS segment : segments )
		{
			removeSegment( segment );
		}
	}

	private synchronized void addSegmentMeshToUniverse( AS segment, CustomTriangleMesh mesh )
	{
		if ( mesh == null )
			throw new RuntimeException( "Mesh of segment " + objectsName + "_" + segment.label() + " is null." );

		if ( universe == null )
			throw new RuntimeException( "Universe is null." );

		final Content content = universe.addCustomMesh( mesh, objectsName + "_" + segment.label() );

		content.setTransparency( ( float ) transparency );
		content.setLocked( true );

		segmentToContent.put( segment, content );
		contentToSegment.put( content, segment );
	}

	private boolean configureUniverseListener()
	{
		universe.addUniverseListener( new UniverseListener()
		{

			@Override
			public void transformationStarted( View view )
			{

			}

			@Override
			public void transformationUpdated( View view )
			{
				// TODO: synchronize with  BDV View

// 			   final Transform3D transform3D = new Transform3D();
//			   view.getUserHeadToVworld( transform3D );

//				final Transform3D transform3D = new Transform3D();
//			    .getVworldToCamera( transform3D );
//				System.out.println( transform3D );

//				final Transform3D transform3DInverse = new Transform3D();
//				.getVworldToCameraInverse( transform3DInverse );
//				System.out.println( transform3DInverse );

//				final TransformGroup transformGroup =
//						.getViewingPlatform()
//								.getMultiTransformGroup().getTransformGroup(
//										DefaultUniverse.ZOOM_TG );
//
//				final Transform3D transform3D = new Transform3D();
//				transformGroup.getTransform( transform3D );
//
//				System.out.println( transform3D );
			}

			@Override
			public void transformationFinished( View view )
			{

			}

			@Override
			public void contentAdded( Content c )
			{

			}

			@Override
			public void contentRemoved( Content c )
			{

			}

			@Override
			public void contentChanged( Content c )
			{

			}

			@Override
			public void contentSelected( Content content )
			{
				if ( content == null || ! contentToSegment.containsKey( content ) )
					return;

				selectionModel.focus( contentToSegment.get( content ), this );
			}

			@Override
			public void canvasResized()
			{

			}

			@Override
			public void universeClosed()
			{
				for ( VisibilityListener listener : listeners )
				{
					listener.visibility( false );
				}
				window = null;
				universe = null;
			}
		} );

		return true;
	}

	private Color3f getColor3f( AS imageSegment )
	{
		final ARGBType argbType = new ARGBType();
		coloringModel.convert( imageSegment, argbType );
		return new Color3f( ColorHelper.getColor( argbType ) );
	}

	public void setVoxelSpacing( double[] voxelSpacing )
	{
		this.voxelSpacing = voxelSpacing;
	}

	public double[] getVoxelSpacing()
	{
		return voxelSpacing;
	}

	public void close()
	{
		showSegments( false, true );
	}

	@Override
	public void coloringChanged()
	{
		updateSegmentColors();
	}

	@Override
	public synchronized void selectionChanged()
	{
		if ( ! showSegments ) return;
		updateView( false );
	}

	@Override
	public synchronized void focusEvent( AS selection, Object initiator )
	{
		if ( ! showSegments ) return;
		if ( initiator == this ) return;
		if ( universe == null ) return;
		if ( universe.getContents().size() == 0 ) return;
		if ( ! segmentToContent.containsKey( selection ) )
		{
			// selected segment is not shown in 3d
			// thus select nothing
			universe.select( null );
			return;
		}

		final Content content = segmentToContent.get( selection );

		if ( content == universe.getSelected() )
		{
			// content is already selected
			return;
		}

		if ( selection.timePoint() != currentTimePoint )
		{
			currentTimePoint = selection.timePoint();
			updateView( false );
		}

		// implement "focus" by setting the content "selected",
		// which will paint a red box around it
		universe.select( content );

		// an alternative (addition) would be to also focus
		// the object by zooming in on it:
		// focus( content )
	}

	// TODO: needs improvement; if appears to first zoom out and then in again
	private void focus( Content content )
	{
		final AnimatedViewAdjuster adjuster =
				new AnimatedViewAdjuster(
						universe,
						AnimatedViewAdjuster.ADJUST_BOTH );

		adjuster.apply( content,
				30,
				segmentFocusAnimationDurationMillis,
				segmentFocusZoomLevel,
				segmentFocusDxyMin,
				segmentFocusDzMin );
	}

	public Collection< VisibilityListener > getListeners()
	{
		return listeners;
	}
}
