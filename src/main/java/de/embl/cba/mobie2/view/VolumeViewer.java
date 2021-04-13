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
package de.embl.cba.mobie2.view;

import bdv.viewer.Source;
import customnode.CustomTriangleMesh;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.Utils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.ij3d.AnimatedViewAdjuster;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class VolumeViewer< S extends ImageSegment > implements ColoringListener, SelectionListener< S >
{
	private final SelectionModel< S > selectionModel;
	private final SelectionColoringModel< S > selectionColoringModel;
	private final ImageSourcesModel imageSourcesModel;

	private Image3DUniverse universe;
	private S recentFocus;
	private ConcurrentHashMap< S, Content > segmentToContent;
	private ConcurrentHashMap< Content, S > contentToSegment;
	private double transparency;
	private boolean isListeningToUniverse;
	private int meshSmoothingIterations;
	private int segmentFocusAnimationDurationMillis;
	private double segmentFocusZoomLevel;
	private double segmentFocusDxyMin;
	private double segmentFocusDzMin;
	private long maxNumSegmentVoxels;
	private String objectsName;
	private Component parentComponent;
	private boolean showSelectedSegments = true;
	private ExecutorService executorService;
	private boolean recomputeMeshes = false;
	private double voxelSpacing = 0; // 0 = auto
	private int currentTimePoint = 0;

	public VolumeViewer(
			final SelectionModel< S > selectionModel,
			final SelectionColoringModel< S > selectionColoringModel,
			ImageSourcesModel imageSourcesModel,
			Image3DUniverse universe )
	{
		this.selectionModel = selectionModel;
		this.selectionColoringModel = selectionColoringModel;
		this.imageSourcesModel = imageSourcesModel;
		this.universe = universe;

		this.transparency = 0.0;
		this.meshSmoothingIterations = 5;
		this.segmentFocusAnimationDurationMillis = 750;
		this.segmentFocusZoomLevel = 0.8;
		this.segmentFocusDxyMin = 20.0;
		this.segmentFocusDzMin = 20.0;
		this.maxNumSegmentVoxels = 100 * 100 * 100;
		this.objectsName = "";
		this.executorService = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
		this.segmentToContent = new ConcurrentHashMap<>();
		this.contentToSegment = new ConcurrentHashMap<>();

	}

	public void setObjectsName( String objectsName )
	{
		if ( objectsName == null )
			throw new RuntimeException( "Cannot set objects name in Segments3dView to null." );

		this.objectsName = objectsName;
	}

	public void setParentComponent( Component parentComponent )
	{
		this.parentComponent = parentComponent;
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

	public Image3DUniverse getUniverse()
	{
		return universe;
	}

	private void updateSegmentColors()
	{
		for ( S segment : segmentToContent.keySet() )
		{
			executorService.submit( () ->
			{
				final Color3f color3f = getColor3f( segment );
				final Content content = segmentToContent.get( segment );
				content.setColor( color3f );
			});
		}
	}

	private synchronized void updateView( boolean recomputeMeshes )
	{
		System.out.println( "Updating view...");
		// TODO: It feels that below functions should be merged...
		updateSelectedSegments( recomputeMeshes );
		removeUnselectedSegments();
		System.out.println( "Updating view: Done.");
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
					// create segment mesh (cheap if it has been done before)
					final CustomTriangleMesh mesh = createSmoothCustomTriangleMesh( segment, voxelSpacing, recomputeMeshes );
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

	private synchronized void removeSegment( S segment )
	{
		final Content content = segmentToContent.get( segment );
		if ( content != null && universe != null )
		{
			universe.removeContent( content.getName() );
			segmentToContent.remove( segment );
			contentToSegment.remove( content );
			System.out.println( "Removed " + getSegmentIdentifier( segment ) );
		}
	}

	private String getSegmentIdentifier( S segment )
	{
		return segment.labelId() + "-" + segment.timePoint();
	}

	public synchronized void showSelectedSegments( boolean showSelectedSegments, boolean recomputeMeshes )
	{
		this.showSelectedSegments = showSelectedSegments;

		if ( showSelectedSegments )
		{
			updateView( recomputeMeshes );
		}
		else
		{
			removeSelectedSegments();
		}
	}

	private void removeSelectedSegments()
	{
		final Set< S > selected = selectionModel.getSelected();

		for ( S segment : selected )
		{
			removeSegment( segment );
		}
	}

	private void addSegmentMeshToUniverse( S segment, CustomTriangleMesh mesh )
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

		System.out.println( "Added " + getSegmentIdentifier( segment ) );


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
//			    universe.getVworldToCamera( transform3D );
//				System.out.println( transform3D );

//				final Transform3D transform3DInverse = new Transform3D();
//				universe.getVworldToCameraInverse( transform3DInverse );
//				System.out.println( transform3DInverse );

//				final TransformGroup transformGroup =
//						universe.getViewingPlatform()
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

			}
		} );

		return true;
	}

	private Color3f getColor3f( S imageSegment )
	{
		final ARGBType argbType = new ARGBType();
		selectionColoringModel.convert( imageSegment, argbType );
		return new Color3f( ColorUtils.getColor( argbType ) );
	}

	public void setVoxelSpacing( double voxelSpacing )
	{
		this.voxelSpacing = voxelSpacing;
	}

	public double getVoxelSpacing()
	{
		return voxelSpacing;
	}

	public void close()
	{
		// TODO
	}

	@Override
	public void coloringChanged()
	{
		updateSegmentColors();
	}

	@Override
	public synchronized void selectionChanged()
	{
		if ( ! showSelectedSegments ) return;

		updateView( false );
	}

	@Override
	public synchronized void focusEvent( S selection )
	{
		if ( ! showSelectedSegments ) return;

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
}
