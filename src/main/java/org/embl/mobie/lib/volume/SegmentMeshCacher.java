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
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
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

import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.util.MoBIEHelper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Pre-renders (and persists) smoothed meshes of all segments of a segmentation
 * display through the disk-backed {@link MeshCache}.
 */
public final class SegmentMeshCacher
{
	private SegmentMeshCacher()
	{}

	/**
	 * Compute and cache smoothed meshes of all segments of the given
	 * segmentation display at the given isotropic voxel spacing.
	 *
	 * @param display   segmentation display whose segments should be cached
	 * @param spacingUm isotropic voxel spacing in µm; if {@code <= 0} the
	 *                  viewer's current (or finest cached) spacing is kept
	 * @return the number of meshes that were newly cached
	 * @throws IllegalStateException if no 3D segment viewer or no mesh cache
	 *                               could be configured for the display
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static int cacheSegmentsAt( SegmentationDisplay display, double spacingUm )
	{
		if ( display.segmentVolumeViewer == null )
			throw new IllegalStateException(
					"The 3D segment viewer is not initialised for display \"" + display.getName() + "\"." );

		if ( spacingUm > 0 )
			display.segmentVolumeViewer.setVoxelSpacing( new double[]{ spacingUm, spacingUm, spacingUm } );

		display.segmentVolumeViewer.configureMeshCache( display.getName(), MoBIEHelper.getMeshCacheDir() );

		if ( display.segmentVolumeViewer.getMeshCache() == null )
			throw new IllegalStateException(
					"No mesh cache could be configured for display \"" + display.getName()
							+ "\". Specify a voxel spacing > 0 (µm) or first cache meshes of this segmentation at a fixed resolution." );

		final Collection segments = segments( display );
		if ( segments.isEmpty() )
			return 0;

		final int cachedBefore = display.segmentVolumeViewer.getMeshCache().size();
		display.segmentVolumeViewer.preRenderSegments( segments );
		return display.segmentVolumeViewer.getMeshCache().size() - cachedBefore;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private static Collection segments( SegmentationDisplay display )
	{
		if ( display.getAnnData() != null && display.getAnnData().getTable() != null )
			return new ArrayList( display.getAnnData().getTable().annotations() );
		return display.selectionModel.getSelected();
	}
}
