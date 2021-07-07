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
package de.embl.cba.mobie.volume;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import customnode.CustomTriangleMesh;
import de.embl.cba.mobie.VisibilityListener;
import de.embl.cba.mobie.mesh.MeshCreator;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.ij3d.AnimatedViewAdjuster;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
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

public class SegmentsVolumeViewer< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	public static String VOLUME_VIEW = "Volume view";

	private final SelectionModel< S > selectionModel;
	private final ColoringModel< S > coloringModel;
	private final Collection< SourceAndConverter< ? > > sourceAndConverters;
	private final UniverseManager universeManager;

	private S recentFocus;
	private ConcurrentHashMap< S, Content > segmentToContent;
	private ConcurrentHashMap< Content, S > contentToSegment;
	private double transparency;
	private int meshSmoothingIterations;
	private int segmentFocusAnimationDurationMillis;
	private double segmentFocusZoomLevel;
	private double segmentFocusDxyMin;
	private double segmentFocusDzMin;
	private long maxNumSegmentVoxels;
	private String objectsName;
	private boolean showSegments = false;
	private double[] voxelSpacing; // desired voxel spacings; null = auto
	private int currentTimePoint = 0;
	private final MeshCreator< ImageSegment > meshCreator;
	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private Window window;
	private Image3DUniverse universe;

	public SegmentsVolumeViewer(
			final SelectionModel< S > selectionModel,
			final ColoringModel< S > coloringModel,
			final Collection< SourceAndConverter< ? > > sourceAndConverters,
			UniverseManager universeManager )
	{
		this.selectionModel = selectionModel;
		this.coloringModel = coloringModel;
		this.sourceAndConverters = sourceAndConverters;
		this.universeManager = universeManager;

		this.transparency = 0.0;
		this.meshSmoothingIterations = 5;
		this.segmentFocusAnimationDurationMillis = 750;
		this.segmentFocusZoomLevel = 0.8;
		this.segmentFocusDxyMin = 20.0;
		this.segmentFocusDzMin = 20.0;
		this.maxNumSegmentVoxels = 100 * 100 * 100;
		this.objectsName = "";
		this.segmentToContent = new ConcurrentHashMap<>();
		this.contentToSegment = new ConcurrentHashMap<>();

		this.meshCreator = new MeshCreator<>( meshSmoothingIterations, maxNumSegmentVoxels );
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

	public void setMaxNumSegmentVoxels( long maxNumSegmentVoxels )
	{
		this.maxNumSegmentVoxels = maxNumSegmentVoxels;
	}

	private void updateSegmentColors()
	{
		for ( S segment : segmentToContent.keySet() )
		{
			final Color3f color3f = getColor3f( segment );
			final Content content = segmentToContent.get( segment );
			content.setColor( color3f );
		}
	}

	private synchronized void updateView( boolean recomputeMeshes )
	{
		// TODO: It feels that below functions should be merged...
		updateSelectedSegments( recomputeMeshes );
		removeUnselectedSegments();
	}

	private void removeUnselectedSegments( )
	{
		final Set< S > selectedSegments = selectionModel.getSelected();
		final Set< S > currentSegments = segmentToContent.keySet();
		final Set< S > remove = new HashSet<>();

		for ( S segment : currentSegments )
			if ( ! selectedSegments.contains( segment ) )
				remove.add( segment );

		for( S segment : remove )
			removeSegment( segment );
	}

	private synchronized void updateSelectedSegments( boolean recomputeMeshes )
	{
		final Set< S > selected = selectionModel.getSelected();

		for ( S segment : selected )
		{
			if ( segment.timePoint() == currentTimePoint )
			{
				if ( recomputeMeshes ) removeSegment( segment );

				if ( ! segmentToContent.containsKey( segment ) )
				{
					final Source< ? extends RealType< ? > > source = getSource( segment );
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

	private Source< ? extends RealType< ? > > getSource( S segment )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getSpimSource().getName().equals( segment.imageId() ))
			{
				return ( Source< ? extends RealType< ? > > ) sourceAndConverter.getSpimSource();
			}
		}

		throw new UnsupportedOperationException( "An image segment from " + segment.imageId() + " did not have a corresponding image source."  );
	}

	private synchronized void removeSegment( S segment )
	{
		final Content content = segmentToContent.get( segment );
		universe.removeContent( content.getName() );
		segmentToContent.remove( segment );
		contentToSegment.remove( content );
	}

	private String getSegmentIdentifier( S segment )
	{
		return segment.labelId() + "-" + segment.timePoint();
	}

	public synchronized void showSegments( boolean showSegments )
	{
		if ( showSegments && universe == null )
		{
			universe = universeManager.get();
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
				new Thread( () -> updateView( false ) ).start();
			}
			else
			{
				new Thread( () -> removeSegments() ).start();
			}
		}
	}

	public boolean getShowSegments() {
		return showSegments;
	}

	private void setShowSegments(boolean b )
	{
		this.showSegments = b;
	}

	private void removeSegments()
	{
		final Set< S > segments = selectionModel.getSelected();

		for ( S segment : segments )
		{
			removeSegment( segment );
		}
	}

	private synchronized void addSegmentMeshToUniverse( S segment, CustomTriangleMesh mesh )
	{
		if ( mesh == null )
			throw new RuntimeException( "Mesh of segment " + objectsName + "_" + segment.labelId() + " is null." );

		if ( universe == null )
			throw new RuntimeException( "Universe is null." );

		final Content content = universe.addCustomMesh( mesh, objectsName + "_" + segment.labelId() );

		content.setTransparency( ( float ) transparency );
		content.setLocked( true );

		segmentToContent.put( segment, content );
		contentToSegment.put( content, segment );

		universe.setAutoAdjustView( false );
	}

	private boolean addUniverseListener()
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
			public void contentSelected( Content c )
			{
				if ( c == null ) return;

				if ( ! contentToSegment.containsKey( c ) )
					return;

				final S segment = contentToSegment.get( c );

				if ( selectionModel.isFocused( segment ) )
				{
					return;
				}
				else
				{
					recentFocus = segment; // avoids "self-focusing"
					selectionModel.focus( segment );
				}
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

	private Color3f getColor3f( S imageSegment )
	{
		final ARGBType argbType = new ARGBType();
		coloringModel.convert( imageSegment, argbType );
		return new Color3f( ColorUtils.getColor( argbType ) );
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
		showSegments( false );
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
	public synchronized void focusEvent( S selection )
	{
		if ( ! showSegments ) return;

		new Thread( () ->
		{
			if ( selection.timePoint() != currentTimePoint )
			{
				currentTimePoint = selection.timePoint();
				updateView( false );
			}
		}).start();

		if ( universe.getContents().size() == 0 ) return;
		if ( selection == recentFocus ) return;
		if ( ! segmentToContent.containsKey( selection ) ) return;

		recentFocus = selection;

		final AnimatedViewAdjuster adjuster =
				new AnimatedViewAdjuster(
						universe,
						AnimatedViewAdjuster.ADJUST_BOTH );

		adjuster.apply(
				segmentToContent.get( selection ),
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
