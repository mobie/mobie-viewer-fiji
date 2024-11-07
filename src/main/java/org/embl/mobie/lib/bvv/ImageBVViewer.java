package org.embl.mobie.lib.bvv;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import bdv.viewer.Source;
import net.imglib2.RandomAccess;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import org.embl.mobie.DataStore;
import org.embl.mobie.lib.image.AnnotationLabelImage;
import org.embl.mobie.lib.image.DefaultAnnotationLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.display.VisibilityListener;

import bdv.viewer.SourceAndConverter;
import btbvv.btuitools.GammaConverterSetup;
import btbvv.vistools.Bvv;
import btbvv.vistools.BvvFunctions;
import btbvv.vistools.BvvHandleFrame;
import btbvv.vistools.BvvStackSource;
import ij.IJ;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.lib.source.AnnotatedLabelSource;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.xml.crypto.Data;


public class ImageBVViewer
{
	//public static final String BVV_VIEWER = "BigVolumeViewer: ";
	private final List< ? extends SourceAndConverter< ? > > sourceAndConverters;
	private ConcurrentHashMap< SourceAndConverter, BvvStackSource > sacToBvvSource;

	private List< VisibilityListener > listeners = new ArrayList<>(  );
	private boolean showImages;
	private final BVVManager bvvManager;
	private Bvv bvv;
	public BvvHandleFrame handle = null;
	


	
	public ImageBVViewer(
			final List< ? extends SourceAndConverter< ? > > sourceAndConverters, BVVManager bvvManager_)
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
		
		int nRenderMethod = 1;
		
		//consistent rendering of all sources
		if(	bvv.getBvvHandle().getViewerPanel().state().getSources().size()>0)
		{
			GammaConverterSetup gConvSetup = ((GammaConverterSetup)bvv.getBvvHandle().getSetupAssignments().getConverterSetups().get( 0 ));	
			nRenderMethod = gConvSetup.getRenderType();
		}
		
		//assume it is always one source
		BvvStackSource< ? >  bvvSource = BvvFunctions.show(BVVSourceToSpimDataWrapper.spimDataSourceWrap( source ), Bvv.options().addTo( bvvManager.get() )).get( 0 );
		
		bvvSource.setRenderType( nRenderMethod );
		double displayRangeMin = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMin();
		double displayRangeMax = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMax();
		final ARGBType color = ( ( ColorConverter ) sac.getConverter() ).getColor();
		
		
		final Object type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
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
		bvvSource.setColor( color );
		
		//handle.getBigVolumeViewer().getViewerFrame().setTitle( BVV_VIEWER + sac.getSpimSource().getName());		
		sacToBvvSource.put( sac, bvvSource );
		//sacToContent.put( sac, content ); ???
		
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
}
