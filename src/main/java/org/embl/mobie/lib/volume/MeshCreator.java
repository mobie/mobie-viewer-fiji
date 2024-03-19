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
package org.embl.mobie.lib.volume;

import bdv.viewer.Source;
import customnode.CustomTriangleMesh;
import de.embl.cba.tables.Utils;
import isosurface.MeshEditor;
import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.type.logic.BitType;
import org.embl.mobie.lib.playground.BdvPlaygroundHelper;
import org.embl.mobie.lib.annotation.Segment;
import org.embl.mobie.lib.source.AnnotationType;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.vecmath.Point3f;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class MeshCreator< S extends Segment >
{
	private final int meshSmoothingIterations;
	private final double maxNumSegmentVoxels;

	public MeshCreator( int meshSmoothingIterations, double maxNumSegmentVoxels )
	{
		this.meshSmoothingIterations = meshSmoothingIterations;
		this.maxNumSegmentVoxels = maxNumSegmentVoxels;
	}

	private float[] createMesh( S segment, @Nullable double[] targetVoxelSpacing, Source< AnnotationType< S > > source )
	{
		int renderingLevel = getLevel( segment, source, targetVoxelSpacing );

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		final int timePoint = segment.timePoint() == null ? 0 : segment.timePoint();
		source.getSourceTransform( timePoint, renderingLevel, sourceTransform );

		final RandomAccessibleInterval< AnnotationType< S > >  rai = source.getSource( timePoint, renderingLevel );

		if ( segment.boundingBox() == null )
		{
			// compute bounding box in voxel space
			//
			RealPoint position;
			try
			{
				position = segment.positionAsRealPoint();
			}
			catch ( Exception e )
			{
				throw new UnsupportedOperationException( "The location of segment " + segment.label() + " could not be determined and thus no mesh could be created;\npossibly the corresponding table has no anchor point entries for this segment" );
			}

			final long[] voxelPositionInSource = SourceAndConverterHelper.getVoxelPositionInSource( source, position, timePoint, renderingLevel );

			final FloodFill floodFill = new FloodFill(
					rai,
					new DiamondShape( 1 ),
					1000 * 1000 * 1000L );

			floodFill.run( voxelPositionInSource );
			final RandomAccessibleInterval< BitType > mask = floodFill.getCroppedRegionMask();

			// set segment bounding box in real space
			//
			final FinalRealInterval realBounds = sourceTransform.estimateBounds( mask );
			segment.setBoundingBox( realBounds );
		}

		Interval voxelBounds = Intervals.smallestContainingInterval( sourceTransform.inverse().estimateBounds( segment.boundingBox() ) );

		if ( ! Intervals.contains( rai, voxelBounds ) )
		{
			System.out.println("Warning: The segment bounding box " + voxelBounds + " is not fully contained in the image interval: " + Arrays.toString( Intervals.minAsLongArray( rai ) ) + "-" +  Arrays.toString( Intervals.maxAsDoubleArray( rai ) ) + "; taking the intersection.");
			voxelBounds = Intervals.intersect( rai, voxelBounds );
		}

		final long numElements = Intervals.numElements( voxelBounds );

		if ( numElements == 0 )
			throw new RuntimeException("The segment is not within the image volume.");

		final AnnotationType< S > type = source.getType();
		final AnnotationType< S > variable = type.createVariable();
		final RandomAccessible< AnnotationType< S > > rra = Views.extendValue( rai, variable );

		final MeshExtractor meshExtractor = new MeshExtractor(
				rra,
				voxelBounds,
				new AffineTransform3D(),
				new int[]{ 1, 1, 1 },
				() -> false );

		final float[] mesh = meshExtractor.extractMesh( new AnnotationType( segment ) );

		if ( mesh.length == 0 )
			throw new RuntimeException("The mesh has zero vertices.");

		// TODO: instead of transformation the mesh, one could
		//  also transform the universe content that is created
		//  from this mesh
		//  Note that for the image volume rendering (@code ImageVolumeViewer)
		//  we need to transform the images as content,
		//  and thus it may be more consistent do to the same for the meshes?!
		final float[] transformedMesh = MeshTransformer.transform( mesh, sourceTransform );

		return transformedMesh;
	}

	public CustomTriangleMesh createSmoothCustomTriangleMesh( S segment, @Nullable double[] voxelSpacing, boolean recomputeMesh, Source< AnnotationType< S > > source )
	{
		CustomTriangleMesh triangleMesh = createCustomTriangleMesh( segment, voxelSpacing, recomputeMesh, source );
		MeshEditor.smooth2( triangleMesh, meshSmoothingIterations );
		return triangleMesh;
	}

	private CustomTriangleMesh createCustomTriangleMesh( S segment, @Nullable double[] voxelSpacing, boolean recomputeMesh, Source< AnnotationType< S > >  source )
	{
		if ( segment.mesh() == null || recomputeMesh )
		{
			try
			{
				segment.setMesh( createMesh( segment, voxelSpacing, source ) );
			}
			catch ( Exception e )
			{
				final String msg = "Could not create mesh for segment " + segment.label() + " at time point " + segment.timePoint();
				//IJ.showMessage( msg );
				throw new RuntimeException( msg );
			}
		}

		CustomTriangleMesh triangleMesh = asCustomTriangleMesh( segment.mesh() );

		return triangleMesh;
	}

	private static CustomTriangleMesh asCustomTriangleMesh( final float[] meshCoordinates )
	{
		final ArrayList< Point3f > points = new ArrayList<>();

		for ( int i = 0; i < meshCoordinates.length; )
		{
			points.add( new Point3f(
					meshCoordinates[ i++ ],
					meshCoordinates[ i++ ],
					meshCoordinates[ i++ ] ) );
		}

		CustomTriangleMesh mesh = new CustomTriangleMesh( points );

		return mesh;
	}

	private Integer getLevel( S segment, Source< ? > labelSource, @Nullable double[] voxelSpacing )
	{
		if ( voxelSpacing != null ) // user determined resolution
		{
			return BdvPlaygroundHelper.getLevel( labelSource, 0, voxelSpacing );
		}
		else // auto-resolution, uses maxNumSegmentVoxels
		{
			if ( segment.boundingBox() == null )
			{
				System.err.println( "3D View:\nAutomated resolution level selection is enabled, but the segment has no bounding box.\nThis combination is currently not supported." );
				throw new RuntimeException();
			}
			else
			{
				int level = getLevel( segment, labelSource );

				return level;
			}
		}
	}

	private int getLevel( S segment, Source< ? > labelSource )
	{
		final ArrayList< double[] > voxelSpacings = Utils.getVoxelSpacings( labelSource );

		final int numLevels = voxelSpacings.size();

		int level;
		for ( level = 0; level < numLevels; level++ )
		{
			FinalInterval boundingBox = getIntervalInVoxelUnits( segment.boundingBox(), voxelSpacings.get( level ) );

			final long numElements = Intervals.numElements( boundingBox );

			if ( numElements <= maxNumSegmentVoxels )
				break;
		}

		if ( level == numLevels ) level = numLevels - 1;
		return level;
	}

	private long[] getSegmentLocationInVoxelsUnits(
			S segment,
			double[] calibration )
	{
		final double[] anchor = segment.positionAsDoubleArray();
		final long[] voxelCoordinate = new long[ anchor.length ];
		for ( int d = 0; d < anchor.length; d++ )
			voxelCoordinate[ d ] = ( long ) ( anchor[ d ] / calibration[ d ] );
		return voxelCoordinate;
	}

	private FinalInterval getIntervalInVoxelUnits(
			RealInterval realInterval,
			double[] calibration )
	{
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			min[ d ] = (long) ( realInterval.realMin( d ) / calibration[ d ] );
			max[ d ] = (long) ( realInterval.realMax( d ) / calibration[ d ] );
		}
		return new FinalInterval( min, max );
	}

}
