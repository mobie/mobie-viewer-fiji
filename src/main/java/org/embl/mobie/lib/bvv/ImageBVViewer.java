package org.embl.mobie.lib.bvv;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bdv.viewer.Source;


import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import org.embl.mobie.DataStore;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.lut.GlasbeyARGBLut;
import org.embl.mobie.lib.image.AnnotationLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.serialize.display.VisibilityListener;

import bdv.viewer.SourceAndConverter;
import bvvpg.pguitools.GammaConverterSetup;
import bvvpg.vistools.Bvv;
import bvvpg.vistools.BvvFunctions;
import bvvpg.vistools.BvvHandleFrame;
import bvvpg.vistools.BvvStackSource;
import ij.IJ;
import mpicbg.spim.data.generic.AbstractSpimData;

import org.embl.mobie.lib.source.AnnotationType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;


public class ImageBVViewer implements ColoringListener, SelectionListener
{
	
	private final List< ? extends SourceAndConverter< ? > > sourceAndConverters;
	@SuppressWarnings( "rawtypes" )
	private ConcurrentHashMap< SourceAndConverter, BvvStackSource > sacToBvvSource;

	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private boolean showImages;
	private final BVVManager bvvManager;
	private Bvv bvv;
	public BvvHandleFrame handle = null;
	private int nRenderMethod;


	public ImageBVViewer(
			final List< ? extends SourceAndConverter< ? > > sourceAndConverters,
			BVVManager bvvManager_)
	{
		this.sourceAndConverters = sourceAndConverters;
		sacToBvvSource = new ConcurrentHashMap<>();
		bvvManager = bvvManager_;
	}
	
	/// is it really needed for now?
	/// seems related to meshes rendering
//	public void updateView()
//	{
//		if ( bvv == null ) return;
//
//		for ( SourceAndConverter< ? > sac : sourceAndConverters )
//		{
//			if ( sacToBvvSource.containsKey( sac ) )
//			{
//				BvvStackSource<?> bvvSource = sacToBvvSource.get( sac );
//				bvvSource.removeFromBdv();
//				sacToBvvSource.remove( sac );
//				addSourceToBVV(sac);
//			}
//		}
//	}

	public synchronized < T > void showImagesBVV( boolean show )
	{
		this.showImages = show;

		if ( showImages && bvv == null )
		{
			// remove previous sources
			sacToBvvSource = new ConcurrentHashMap<>();
			initBVV();
		}
		
		for ( SourceAndConverter< ? > sac : sourceAndConverters )
		{
			if ( sacToBvvSource.containsKey( sac ) )
			{
				sacToBvvSource.get( sac ).setActive( show );
			}
			else
			{
				if ( show )
				{
					addSourceToBVV(sac);
				}
			}
		}
	}
	
	void addSourceToBVV(SourceAndConverter< ? > sac)
	{
		Source< ? > source = getSource( sac );
		final AbstractSpimData< ? > spimData = BVVSourceToSpimDataWrapper.spimDataSourceWrap( source );
		
		if(spimData == null)
		{
			RandomAccess< ? > randomAccess = source.getSource( 0, 0 ).randomAccess();
			IJ.log( "Cannot display " + source.getName() + " in BVV, incompatible data type:\n" +
					randomAccess.get().getClass().getName() );
			return;
		}

		nRenderMethod = 1;
		
		//consistent rendering of all sources
		if(	bvv.getBvvHandle().getViewerPanel().state().getSources().size()>0)
		{
			@SuppressWarnings( "deprecation" )
			GammaConverterSetup gConvSetup = ((GammaConverterSetup)bvv.getBvvHandle().getSetupAssignments().getConverterSetups().get( 0 ));	
			nRenderMethod = gConvSetup.getRenderType();
		}
		
		//assume it is always one source
		BvvStackSource< ? >  bvvSource = BvvFunctions.show(
				BVVSourceToSpimDataWrapper.spimDataSourceWrap( source ),
				Bvv.options().addTo( bvvManager.get() )).get( 0 );
		sacToBvvSource.put( sac, bvvSource );

		configureRenderingSettings( sac, bvvSource );
	}

	private void configureRenderingSettings(
			SourceAndConverter< ? > sac,
			BvvStackSource< ? > bvvSource )
	{
		bvvSource.setRenderType( nRenderMethod );
		double displayRangeMin = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMin();
		double displayRangeMax = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMax();
		if(isAnnotation( sac ))
		{
			final IndexColorModel icmAnnLUT = getAnnotationLUT( sac );
			//final IndexColorModel icmAnnLUT = getAnnotationLUTTwoLabelsExample(sac);
			bvvSource.setLUT(icmAnnLUT,Integer.toString( icmAnnLUT.hashCode()));
			bvvSource.setDisplayRangeBounds( 0, icmAnnLUT.getMapSize()-1);
			bvvSource.setDisplayRange( 0, icmAnnLUT.getMapSize()-1);
			bvvSource.setAlphaRangeBounds( 0,1);
			bvvSource.setAlphaRange( 0,1);
			bvvSource.setVoxelRenderInterpolation( 0 );
		}
		else
		{
			final ARGBType color = ( ( ColorConverter ) sac.getConverter() ).getColor();

			bvvSource.setColor( color );
			final Object type = Util.getTypeFromInterval( sac.getSpimSource().getSource( 0, 0 ) );
			final double[] contrastLimits = new double[ 2 ];
			contrastLimits[ 0 ] = 0;
			if ( type instanceof UnsignedByteType )
				contrastLimits[ 1 ] = 255;
			else
				contrastLimits[ 1 ] = 65535;
			//is maybe Double needed? probably not
			if(type instanceof FloatType)
			{
				displayRangeMin = 0.;
				displayRangeMax = 65535;
			}
			bvvSource.setDisplayRangeBounds( contrastLimits[0], contrastLimits[1]);
			bvvSource.setDisplayRange( displayRangeMin, displayRangeMax );
		}
	}

	private static boolean isAnnotation( SourceAndConverter< ? > sac )
	{
		if ( DataStore.getImage( sac.getSpimSource().getName() ) instanceof AnnotationLabelImage )
		{
			return true;
		}
		return false;
	}
	
	private static Source< ? > getSource( SourceAndConverter< ? > sac )
	{
		Image< ? > image = DataStore.getImage( sac.getSpimSource().getName() );
		Source< ? > source;
		
		if ( image instanceof AnnotationLabelImage )
		{
			source = ( ( AnnotationLabelImage<?> ) image ).getLabelImage().getSourcePair().getSource();
		}
		else
		{
			source = sac.getSpimSource();
		}
		return source;
	}

	void initBVV()
	{
		bvv = bvvManager.get();
		handle = (BvvHandleFrame)bvv.getBvvHandle();
		handle.getBigVolumeViewer().getViewerFrame().addWindowListener(  
				new WindowAdapter()
				{
					@Override
					public void windowClosing( WindowEvent ev )
					{
						bvv = null;
						sacToBvvSource.clear();
						showImages = false;
						//handle.close();
						handle = null;
						bvvManager.setBVV( null );
						for ( VisibilityListener listener : listeners )
						{
							listener.visibility( false );
						}
					}
				});
	}
	
	public void close()
	{
		if(handle!=null)
		{
			bvv = null;
			sacToBvvSource.clear();
			handle.close();
			// not really sure how to close it without Painter thread exception,
			// but in reality it can just be ignored
//			handle.getViewerPanel().stop();
//			try
//			{
//				Thread.sleep( 100 );
//			}
//			catch ( InterruptedException exc )
//			{
//				exc.printStackTrace();
//			}
//			handle.getBigVolumeViewer().getViewerFrame().dispose();
		}
	}

	public Collection< VisibilityListener > getListeners()
	{
		return listeners;
	}
	
	public boolean getShowImages() { return showImages; }
	
	public static IndexColorModel getGlasbeyICM()
	{
		final GlasbeyARGBLut gARGB = new GlasbeyARGBLut();
		final byte [][] colors = new byte [3][256];
		int val;
		for(int i=0;i<256;i++)
		{
			val =gARGB.getARGB( i );
			colors[0][i] = ( byte ) ARGBType.red( val );
			colors[1][i] = ( byte ) ARGBType.green( val );
			colors[2][i] = ( byte ) ARGBType.blue( val );
		}

		return new IndexColorModel(8,256,colors[0],colors[1],colors[2]);
	}
	
	/** returns RGB LUT from the annotation image, i.e. UnsignedLongType
	 * value to RGB. The size of LUT is #of labels + 1, 
	 * since it adds Color.BLACK as zero values. 
	 * Works only if the number of labels is <=65535 **/
	
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static IndexColorModel getAnnotationLUT(SourceAndConverter< ? > sac)
	{
		Image< ? > image = DataStore.getImage( sac.getSpimSource().getName() );
		if ( image instanceof AnnotationLabelImage )
		{
			AnnotationAdapter< Annotation > annotationAdapter = ( ( AnnotationLabelImage< Annotation > ) image ).getAnnotationAdapter();
			Converter< AnnotationType, ARGBType > converter = ( Converter< AnnotationType, ARGBType > ) sac.getConverter();

			final int nAnnotationsNumber = ( ( AnnotationLabelImage<?> ) image ).getAnnData().getTable().numAnnotations();
			
			final byte [][] colors = new byte [3][nAnnotationsNumber+1];
			final byte [] alphas = new byte [nAnnotationsNumber+1];
			ARGBType valARGB = new ARGBType();
			int val;
			
			// 0 is background is black
			colors[0][0] = 0;
			colors[1][0] = 0;
			colors[2][0] = 0;
			alphas[0] = ( byte ) ( 0 );
			int timePoint = 0;
			for(int label=1; label<=nAnnotationsNumber; label++)
			{
				final Annotation annotation = annotationAdapter.getAnnotation( image.getName(), timePoint, label );
				converter.convert( new AnnotationType<>( annotation ), valARGB );
				val = valARGB.get();
				colors[ 0 ][ label ] = ( byte ) ARGBType.red( val );
				colors[ 1 ][ label ] = ( byte ) ARGBType.green( val );
				colors[ 2 ][ label ] = ( byte ) ARGBType.blue( val );
				alphas[ label ] = (byte) ARGBType.alpha( val );
			}
			return new IndexColorModel(16,nAnnotationsNumber+1,colors[0],colors[1],colors[2], alphas);
		}
		return null;
	}

	@Override
	public void coloringChanged()
	{
		for ( Map.Entry< SourceAndConverter, BvvStackSource > entry : sacToBvvSource.entrySet() )
		{
			configureRenderingSettings( entry.getKey(), entry.getValue() );
		}
		bvv.getBvvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	public void selectionChanged()
	{
		for ( Map.Entry< SourceAndConverter, BvvStackSource > entry : sacToBvvSource.entrySet() )
		{
			if ( isAnnotation( entry.getKey() ) )
				configureRenderingSettings( entry.getKey(), entry.getValue() );
		}
		bvv.getBvvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	public void focusEvent( Object selection, Object initiator )
	{

	}
}
