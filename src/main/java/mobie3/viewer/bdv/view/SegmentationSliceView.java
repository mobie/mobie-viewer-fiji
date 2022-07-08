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
package mobie3.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mobie3.viewer.MoBIE;
import mobie3.viewer.color.AnnotationConverter;
import mobie3.viewer.color.VolatileAnnotationConverter;
import mobie3.viewer.display.SegmentationDisplay;
import mobie3.viewer.segment.SegmentAdapter;
import mobie3.viewer.segment.SliceViewAnnotationSelector;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.SegmentSource;
import mobie3.viewer.source.VolatileAnnotationType;
import mobie3.viewer.source.VolatileBoundarySource;
import mobie3.viewer.source.VolatileSegmentationSource;
import mobie3.viewer.transform.SliceViewLocationChanger;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;

public class SegmentationSliceView< N extends NumericType< N > & RealType< N > > extends AnnotationSliceView< TableRowImageSegment >
{
	public SegmentationSliceView( MoBIE moBIE, SegmentationDisplay display )
	{
		super( moBIE, display );

		for ( String name : display.getSources() )
		{
			final SourceAndConverter< N > sourceAndConverter = ( SourceAndConverter< N > ) moBIE.sourceNameToSourceAndConverter().get( name );

			SourceAndConverter< ? > labelSourceAndConverter = createSegmentationSourceAndConverter( sourceAndConverter, display );

			show( labelSourceAndConverter );
		}
	}

	private SourceAndConverter createSegmentationSourceAndConverter( SourceAndConverter< N > sourceAndConverter, SegmentationDisplay display )
	{
		// TODO: only do the converter stuff here, the rest will be done during opening!

		// TODO: in fact this could contain more table rows than needed...
		final SegmentAdapter< TableRowImageSegment > adapter = new SegmentAdapter<>( display.tableRows.getTableRows() );

		// volatile
		final Source< ? extends Volatile< N > > volatileSpimSource = sourceAndConverter.asVolatile().getSpimSource();
		final Source< VolatileAnnotationType< TableRowImageSegment > > volatileSegmentationSource = new VolatileSegmentationSource( volatileSpimSource, adapter );
		final VolatileBoundarySource volatileBoundarySource = new VolatileBoundarySource( volatileSegmentationSource );
		final VolatileAnnotationConverter volatileAnnotationConverter = new VolatileAnnotationConverter( display.selectionColoringModel );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileBoundarySource, volatileAnnotationConverter );

		// non-volatile
		final Source< N > spimSource = sourceAndConverter.getSpimSource();
		final SegmentSource< N, TableRowImageSegment > segmentSource = new SegmentSource<>( spimSource, adapter );
		final BoundarySource boundarySource = new BoundarySource( segmentSource );
		final AnnotationConverter< TableRowImageSegment > annotationConverter = new AnnotationConverter<>( display.selectionColoringModel );

		// combined
		return new SourceAndConverter( boundarySource, annotationConverter, volatileSourceAndConverter );
	}

	@Override
	public synchronized void focusEvent( TableRowImageSegment selection, Object initiator )
	{
		if ( initiator instanceof SliceViewAnnotationSelector )
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		state.setCurrentTimepoint( selection.timePoint() );

		final double[] position = getPosition( selection );

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				SliceViewLocationChanger.animationDurationMillis ).run();
	}

	private double[] getPosition( TableRowImageSegment segment )
	{
		final double[] position = new double[ 3 ];
		segment.localize( position );
		final SourceAndConverter< ? > sourceAndConverter = moBIE.sourceNameToSourceAndConverter().get( segment.imageId()  );

		// get source transform
		AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );

		// get voxel dimensions
		final VoxelDimensions voxelDimensions;
		voxelDimensions = sourceAndConverter.getSpimSource().getVoxelDimensions();

		// remove scaling from source transform,
		// because position is already in scaled units
		final AffineTransform3D scalingTransform = new AffineTransform3D();
		scalingTransform.scale( voxelDimensions.dimension( 0 ), voxelDimensions.dimension( 1 ), voxelDimensions.dimension( 2 )  );
		sourceTransform.concatenate( scalingTransform.inverse() );

		// move position according current source location
		sourceTransform.apply( position, position );

		return position;
	}
}
