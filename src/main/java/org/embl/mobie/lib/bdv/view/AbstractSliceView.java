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
package org.embl.mobie.lib.bdv.view;

import bdv.TransformEventHandler3D;
import bdv.util.BdvHandle;
import bdv.util.PlaceHolderSource;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.serialize.display.AbstractDisplay;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.List;
import java.util.Optional;

public abstract class AbstractSliceView implements SliceView
{
	protected final MoBIE moBIE;
	protected final AbstractDisplay< ? > display;
	protected final SliceViewer sliceViewer;

	protected static boolean is2D = false;
	private final BehaviourMap blocking3dBehaviourMap;
	private Behaviour dragRotate;

	// TODO: get rid of MoBIE here, which is only needed to close the sacs...
	//  in fact, using Nico's addition to the SACService will resolve this!
	//  see the corresponding issue: https://github.com/bigdataviewer/bigdataviewer-playground/issues/259#issuecomment-1279705489
	public AbstractSliceView( MoBIE moBIE, AbstractDisplay< ? > display )
	{
		this.moBIE = moBIE;
		this.display = display;
		this.sliceViewer = display.sliceViewer;

		blocking3dBehaviourMap = new BehaviourMap();
		blocking3dBehaviourMap.put( TransformEventHandler3D.DRAG_ROTATE, new Behaviour() {} );
		blocking3dBehaviourMap.put( TransformEventHandler3D.DRAG_ROTATE_FAST, new Behaviour() {} );
		blocking3dBehaviourMap.put( TransformEventHandler3D.DRAG_ROTATE_SLOW, new Behaviour() {} );
		blocking3dBehaviourMap.put( TransformEventHandler3D.SCROLL_Z, new Behaviour() {} );
		blocking3dBehaviourMap.put( TransformEventHandler3D.SCROLL_Z_FAST, new Behaviour() {} );
		blocking3dBehaviourMap.put( TransformEventHandler3D.SCROLL_Z_SLOW, new Behaviour() {} );
	}

	@Override
	public void close( boolean closeImgLoader )
	{
		final List< ? extends SourceAndConverter< ? > > sourceAndConverters = display.sourceAndConverters();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			// TODO https://github.com/bigdataviewer/bigdataviewer-playground/issues/259#issuecomment-1279705489
			moBIE.closeSourceAndConverter( sourceAndConverter, closeImgLoader );
		}
		display.images().clear();

		sliceViewer.updateTimepointSlider();
	}

	@Override
	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	@Override
	public boolean isVisible()
	{
		return SourceAndConverterServices.getBdvDisplayService().isVisible( display.sourceAndConverters().get( 0 ), display.sliceViewer.getBdvHandle() );
	}

	protected synchronized void adjust2d3dBrowsingMode()
	{
		if ( getSliceViewer().is2D() )
		{
			// we don't want to disturb legacy settings of the 2D mode
			return;
		}

		// Determine whether there is a 3D source
		//
		BdvHandle bdvHandle = sliceViewer.getBdvHandle();
		List< SourceAndConverter< ? > > sources = bdvHandle.getViewerPanel().state().getSources();
		Optional< SourceAndConverter< ? > > source3D = sources.stream()
				.filter( s -> ! ( DataStore.sourceToImage().get( s ) instanceof RegionAnnotationImage ) )
				.filter( s -> ! ( s.getSpimSource() instanceof PlaceHolderSource ) )
				.filter( s -> s.getSpimSource().getSource( 0, 0 ).dimension( 2 ) > 1 )
				.findFirst();

		// https://forum.image.sc/t/switch-bigdataviewer-browsing-mode-on-the-fly/119921
		if ( source3D.isPresent() )
		{
			if ( !is2D ) return;

			bdvHandle.getTriggerbindings().removeBehaviourMap( "2D" );

			is2D = false;
			IJ.log("BDV: 3D browsing mode.");
		}
		else
		{
			if ( is2D ) return;

			bdvHandle.getTriggerbindings().addBehaviourMap( "2D", blocking3dBehaviourMap );

			is2D = true;
			IJ.log("BDV: 2D browsing mode.");
		}
	}
}
