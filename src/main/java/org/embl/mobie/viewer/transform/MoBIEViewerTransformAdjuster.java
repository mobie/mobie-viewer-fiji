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
package org.embl.mobie.viewer.transform;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SynchronizedViewerState;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

import java.util.List;

public class MoBIEViewerTransformAdjuster {
	private final BdvHandle bdvHandle;
	private final List< Source< ? > > sources;

	public MoBIEViewerTransformAdjuster( BdvHandle bdvHandle, List< Source< ? > > sources) {
		this.bdvHandle = bdvHandle;
		this.sources = sources;
	}

	public AffineTransform3D getSingleSourceTransform()
	{
		SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		int viewerWidth = bdvHandle.getBdvHandle().getViewerPanel().getWidth();
		int viewerHeight = bdvHandle.getBdvHandle().getViewerPanel().getHeight();
		double cX = (double)viewerWidth / 2.0D;
		double cY = (double)viewerHeight / 2.0D;
		int timepoint = state.getCurrentTimepoint();
		Source< ? > source = sources.get( 0 );
		if (! source.isPresent(timepoint)) {
			return new AffineTransform3D();
		} else {
			AffineTransform3D sourceTransform = new AffineTransform3D();
			source.getSourceTransform(timepoint, 0, sourceTransform);
			Interval sourceInterval = source.getSource(timepoint, 0);
			double sX0 = (double)sourceInterval.min(0);
			double sX1 = (double)sourceInterval.max(0);
			double sY0 = (double)sourceInterval.min(1);
			double sY1 = (double)sourceInterval.max(1);
			double sZ0 = (double)sourceInterval.min(2);
			double sZ1 = (double)sourceInterval.max(2);
			double sX = (sX0 + sX1) / 2.0D;
			double sY = (sY0 + sY1) / 2.0D;
			double sZ = (double)Math.round((sZ0 + sZ1) / 2.0D);
			double[][] m = new double[3][4];
			double[] qSource = new double[4];
			double[] qViewer = new double[4];

			Affine3DHelpers.extractApproximateRotationAffine(sourceTransform, qSource, 2);
			LinAlgHelpers.quaternionInvert(qSource, qViewer);
			LinAlgHelpers.quaternionToR(qViewer, m);

			double[] centerSource = new double[]{sX, sY, sZ};
			double[] centerGlobal = new double[3];
			double[] translation = new double[3];
			sourceTransform.apply(centerSource, centerGlobal);
			double[] pSource = new double[]{sX1 + 0.5D, sY1 + 0.5D, sZ};
			double[] pGlobal = new double[3];
			double[] pScreen = new double[3];
			sourceTransform.apply(pSource, pGlobal);


			LinAlgHelpers.quaternionApply(qViewer, centerGlobal, translation);
			LinAlgHelpers.scale(translation, -1.0D, translation);
			LinAlgHelpers.setCol(3, translation, m);
			AffineTransform3D viewerTransform = new AffineTransform3D();
			viewerTransform.set(m);
			viewerTransform.apply(pGlobal, pScreen);

			double scaleX = cX / pScreen[0];
			double scaleY = cY / pScreen[1];
			double scale = Math.min(scaleX, scaleY);
			viewerTransform.scale(scale);
			viewerTransform.set(viewerTransform.get(0, 3) + cX - 0.5D, 0, 3);
			viewerTransform.set(viewerTransform.get(1, 3) + cY - 0.5D, 1, 3);
			return viewerTransform;
		}
	}

	public AffineTransform3D getMultiSourceTransform() {
		SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		final RealInterval bounds = TransformHelper.estimateBounds( sources, state.getCurrentTimepoint() );
		final AffineTransform3D transform = TransformHelper.getIntervalViewerTransform( bdvHandle, bounds );
		return transform;
	}

}

