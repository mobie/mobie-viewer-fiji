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
package mobie3.viewer.volume;

import bdv.viewer.Source;
import customnode.CustomTriangleMesh;
import de.embl.cba.bdv.utils.objects3d.FloodFill;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.Utils;
import isosurface.MeshEditor;
import mobie3.viewer.playground.BdvPlaygroundHelper;
import mobie3.viewer.annotation.Segment;
import mobie3.viewer.source.AnnotationType;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class MeshCreator< S extends Segment >
{
	private int meshSmoothingIterations;
	private double maxNumSegmentVoxels;

	public MeshCreator( int meshSmoothingIterations, double maxNumSegmentVoxels )
	{
		this.meshSmoothingIterations = meshSmoothingIterations;
		this.maxNumSegmentVoxels = maxNumSegmentVoxels;
	}

	private float[] createMesh( S segment, @Nullable double[] targetVoxelSpacing, Source< AnnotationType< S > > source )
	{
		Integer level = getLevel( segment, source, targetVoxelSpacing );

		final RandomAccessibleInterval< AnnotationType< S > >  rai = source.getSource( segment.timePoint(), level );
		double[] sourceVoxelSpacing = Utils.getVoxelSpacings( source ).get( level );

		if ( segment.boundingBox() == null )
			computeSegmentBoundingBox( segment, rai, sourceVoxelSpacing );

		FinalInterval boundingBox = getIntervalInVoxelUnits( segment.boundingBox(), sourceVoxelSpacing );
		final long numElements = Intervals.numElements( boundingBox );

		if ( targetVoxelSpacing == null ) // auto-resolution
		{
			if ( numElements > maxNumSegmentVoxels )
			{
				Logger.info( "# 3D View:\n" +
						"The bounding box of the selected segment has " + numElements + " voxels.\n" +
						"The maximum recommended number is however only " + maxNumSegmentVoxels + ".\n" +
						"It can take a bit of time to load...." );
			}
		}

		if ( ! Intervals.contains( rai, boundingBox ) )
		{
			System.err.println( "The segment bounding box " + boundingBox + " is not fully contained in the image interval: " + Arrays.toString( Intervals.minAsLongArray( rai ) ) + "-" +  Arrays.toString( Intervals.maxAsDoubleArray( rai ) ));
		}
		final AnnotationType< S > type = source.getType();
		final AnnotationType< S > variable = type.createVariable();
		final RandomAccessible< AnnotationType< S > > extendValue = Views.extendValue( rai, variable );

		final MeshExtractor meshExtractor = new MeshExtractor(
				extendValue,
				boundingBox,
				new AffineTransform3D(),
				new int[]{ 1, 1, 1 },
				() -> false );

		final float[] meshCoordinates = meshExtractor.generateMesh( new AnnotationType( segment ) );

		for ( int i = 0; i < meshCoordinates.length; )
		{
			meshCoordinates[ i++ ] *= sourceVoxelSpacing[ 0 ];
			meshCoordinates[ i++ ] *= sourceVoxelSpacing[ 1 ];
			meshCoordinates[ i++ ] *= sourceVoxelSpacing[ 2 ];
		}

		if ( meshCoordinates.length == 0 )
		{
			throw new RuntimeException("Mesh has zero pixels.");
		}

		return meshCoordinates;
	}

	public CustomTriangleMesh createSmoothCustomTriangleMesh( S segment, double[] voxelSpacing, boolean recomputeMesh, Source< AnnotationType< S > >  source )
	{
		CustomTriangleMesh triangleMesh = createCustomTriangleMesh( segment, voxelSpacing, recomputeMesh, source );
		MeshEditor.smooth2( triangleMesh, meshSmoothingIterations );
		return triangleMesh;
	}

	private CustomTriangleMesh createCustomTriangleMesh( S segment, double[] voxelSpacing, boolean recomputeMesh, Source< AnnotationType< S > >  source )
	{
		if ( segment.mesh() == null || recomputeMesh )
		{
			try
			{
				segment.setMesh( createMesh( segment, voxelSpacing, source ) );
			}
			catch ( Exception e )
			{
				final String msg = "Could not create mesh for segment " + segment.labelId() + " at time point " + segment.timePoint();
				//IJ.showMessage( msg );
				e.printStackTrace();
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
			return BdvPlaygroundHelper.getLevel( labelSource, voxelSpacing );
		}
		else // auto-resolution, uses maxNumSegmentVoxels
		{
			if ( segment.boundingBox() == null )
			{
				Logger.error( "3D View:\nAutomated resolution level selection is enabled, but the segment has no bounding box.\nThis combination is currently not supported." );
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

	private void computeSegmentBoundingBox(
			S segment,
			RandomAccessibleInterval< AnnotationType< S > >  rai,
			double[] voxelSpacing )
	{
		final long[] voxelCoordinate = getSegmentLocationInVoxelsUnits( segment, voxelSpacing );

		final FloodFill floodFill = new FloodFill(
				rai,
				new DiamondShape( 1 ),
				1000 * 1000 * 1000L );

		floodFill.run( voxelCoordinate );
		final RandomAccessibleInterval mask = floodFill.getCroppedRegionMask();

		final int numDimensions = segment.anchor().length;
		final double[] min = new double[ numDimensions ];
		final double[] max = new double[ numDimensions ];
		for ( int d = 0; d < numDimensions; d++ )
		{
			min[ d ] = mask.min( d ) * voxelSpacing[ d ];
			max[ d ] = mask.max( d ) * voxelSpacing[ d ];
		}

		segment.setBoundingBox( new FinalRealInterval( min, max ) );
	}

	private long[] getSegmentLocationInVoxelsUnits(
			S segment,
			double[] calibration )
	{
		final double[] anchor = segment.anchor();
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
