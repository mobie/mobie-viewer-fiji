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
import bdv.viewer.SourceAndConverter;
import de.embl.cba.util.CopyUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import ij3d.UniverseListener;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.playground.BdvPlaygroundHelper;
import org.embl.mobie.lib.serialize.display.VisibilityListener;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ImageVolumeViewer
{
	public static final String VOLUME_VIEWER = "Volume Viewer: ";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final List< ? extends SourceAndConverter< ? > > sourceAndConverters;
	private final UniverseManager universeManager;
	private ConcurrentHashMap< SourceAndConverter, Content > sacToContent;
	private ConcurrentHashMap< Content, SourceAndConverter > contentToSac;
	private boolean showImages;
	private int meshSmoothingIterations;
	private long maxNumVoxels;
	private double[] voxelSpacing; // desired voxel spacings; null = auto => use maxNumVoxels
	private float transparency = 0.0F;
	private int currentTimePoint = 0;
	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private Window window;
	private Image3DUniverse universe;

	public ImageVolumeViewer(
			final List< ? extends SourceAndConverter< ? > > sourceAndConverters,
			UniverseManager universeManager )
	{
		this.sourceAndConverters = sourceAndConverters;
		this.universeManager = universeManager;

		this.meshSmoothingIterations = 5;
		this.maxNumVoxels = 200 * 200 * 200;
		this.sacToContent = new ConcurrentHashMap<>();
		this.contentToSac = new ConcurrentHashMap<>();
	}

	public void setMaxNumVoxels( long maxNumVoxels )
	{
		this.maxNumVoxels = maxNumVoxels;
	}

	public void updateView()
	{
		if ( universe == null ) return;

		for ( SourceAndConverter< ? > sac : sourceAndConverters )
		{
			if ( sacToContent.containsKey( sac ) )
			{
				final Content content = sacToContent.get( sac );
				universe.removeContent( content.getName() );
				sacToContent.remove( sac );
				if ( content.isVisible() )
					addSourceToUniverse( sac );
			}
		}
	}

	public synchronized void showImages( boolean show )
	{
		this.showImages = show;

		if ( showImages && universe == null )
		{
			initUniverse();
		}

		for ( SourceAndConverter< ? > sac : sourceAndConverters )
		{
			if ( sacToContent.containsKey( sac ) )
			{
				sacToContent.get( sac ).setVisible( show );
			}
			else
			{
				if ( show )
				{
					addSourceToUniverse( sac );
				}
			}
		}
	}

	private void addSourceToUniverse( SourceAndConverter< ? > sac )
	{
		final double displayRangeMin = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMin();
		final double displayRangeMax = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMax();
		final double[] contrastLimits = { displayRangeMin, displayRangeMax };
		final ARGBType color = ( ( ColorConverter ) sac.getConverter() ).getColor();
		final Content content = addSourceToUniverse( universe, sac.getSpimSource(), voxelSpacing, maxNumVoxels, ContentConstants.VOLUME, color, transparency, contrastLimits );
		universe.adjustView( content );
		sacToContent.put( sac, content );
	}

	public < R extends RealType< R > > Content addSourceToUniverse(
			Image3DUniverse universe,
			Source< ? > source,
			double[] voxelSpacing,
			long maxNumVoxels,
			int displayType,
			ARGBType argbType,
			float transparency,
			double[] contrastLimits )
	{
		if ( universe == null  )
		{
			IJ.log( "[ERROR] No Universe exists => Cannot show volume." );
			return null;
		}

		if ( universe.getWindow() == null )
		{
			IJ.log( "[ERROR] No Universe window exists => Cannot show volume." );
			return null;
		}

		int resolutionLevel = -1;
		if ( voxelSpacing != null )
			resolutionLevel = BdvPlaygroundHelper.getLevel( source, currentTimePoint, voxelSpacing );
		else
			resolutionLevel = BdvPlaygroundHelper.getLevel( source, currentTimePoint, maxNumVoxels );

		if ( resolutionLevel == -1 )
		{
			IJ.log( "[ERROR] Image is too large to be displayed in 3D." );
			return null;
		}

		final double[] voxelSpacings = BdvPlaygroundHelper.getVoxelSpacing( source, currentTimePoint, resolutionLevel );
		IJ.log( VOLUME_VIEWER +  source.getName() + " voxel spacing @ resolution level " + resolutionLevel + ": "
				+ Arrays.toString( voxelSpacings ) + " " + source.getVoxelDimensions().unit() );

		final ImagePlus imagePlus = createUnsignedByteImagePlus( source, currentTimePoint, resolutionLevel, contrastLimits );
		final Content content = universe.addContent( imagePlus, displayType );

		// transform (including voxel size)
		AffineTransform3D affineTransform3D = new AffineTransform3D();
		source.getSourceTransform( currentTimePoint, resolutionLevel, affineTransform3D );
		Transform3D transform3D = new Transform3D();
		double[] rowPackedCopy = affineTransform3D.getRowPackedCopy();
		double[] newValues = { 0, 0, 0, 1 }; // add perspective transformation
		double[] sixteenValues = new double[rowPackedCopy.length + newValues.length];
		System.arraycopy( rowPackedCopy, 0, sixteenValues, 0, rowPackedCopy.length );
		System.arraycopy( newValues, 0, sixteenValues, rowPackedCopy.length, newValues.length );
		transform3D.set( sixteenValues );
		IJ.log( VOLUME_VIEWER + source.getName() + " transformation:\n" + transform3D );
		content.applyTransform( transform3D );

		Color3f color = new Color3f( ColorHelper.getColor( argbType ) );
		content.setColor( color );
		content.setTransparency( transparency );
		content.setLocked( true );

		return content;
	}

	private static < R extends RealType< R > & NativeType< R > > ImagePlus createUnsignedByteImagePlus( Source< ? > source, int currentTimePoint, int resolutionLevel, double[] contrastLimits )
	{
		RandomAccessibleInterval< R > rai = ( RandomAccessibleInterval< R > ) source.getSource( currentTimePoint, resolutionLevel );
		IJ.log( VOLUME_VIEWER + source.getName() + " loading... "  );
		rai = CopyUtils.copyVolumeRaiMultiThreaded( rai, Prefs.getThreads() - 1  ); // TODO: make multi-threading configurable.
		IJ.log( VOLUME_VIEWER + source.getName() + " shape: " + Arrays.toString( rai.dimensionsAsLongArray() ) );
		rai = Views.permute( Views.addDimension( rai, 0, 0 ), 2, 3 );

		final ImagePlus imagePlus = ImageJFunctions.wrapUnsignedByte(
				rai,
				new RealUnsignedByteConverter< R >( contrastLimits[ 0 ], contrastLimits[ 1 ] ),
				source.getName() );

		// we don't calibrate here but do this during the content transformation
		//		wrap.getCalibration().pixelWidth = voxelSpacings[ 0 ];
		//		wrap.getCalibration().pixelHeight = voxelSpacings[ 1 ];
		//		wrap.getCalibration().pixelDepth = voxelSpacings[ 2 ];
		//		wrap.getCalibration().setUnit( source.getVoxelDimensions().unit() );

		return imagePlus;
	}

	private int[] getContrastLimits( SourceAndConverter< ? > sac )
	{
		final Object type = Util.getTypeFromInterval( sac.getSpimSource().getSource( 0, 0 ) );
		final int[] contrastLimits = new int[ 2 ];
		contrastLimits[ 0 ] = 0;
		if ( type instanceof UnsignedByteType )
			contrastLimits[ 1 ] = 255;
		else if ( type instanceof UnsignedShortType )
			contrastLimits[ 1 ] = 65535;
		else
			throw new RuntimeException( "Volume view of image of type " + type + " is currently not supported.");
		return contrastLimits;
	}

	private void initUniverse()
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
					sacToContent.clear();
					contentToSac.clear();
					showImages = false;
					universeManager.setUniverse( null );
					for ( VisibilityListener listener : listeners )
					{
						listener.visibility( false );
					}
				}
			} );
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
				// TODO: synchronize with BDV view could be nice...
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

				if ( ! contentToSac.containsKey( c ) )
					return;

				// Should we do anything?
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

	public boolean setVoxelSpacing( double[] voxelSpacing )
	{
		if ( this.voxelSpacing == null && voxelSpacing == null )
			return false;

		if ( Arrays.equals( this.voxelSpacing, voxelSpacing ) )
			return false;

		this.voxelSpacing = voxelSpacing;
		return true; // voxel spacing changed
	}

	public double[] getVoxelSpacing()
	{
		return voxelSpacing;
	}

	public boolean getShowImages() { return showImages; }

	public void close()
	{
		showImages( false );
	}

	public Collection< VisibilityListener > getListeners()
	{
		return listeners;
	}
}
