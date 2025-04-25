package org.embl.mobie.lib.bvb;


import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.AnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotationAdapter;

import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.lut.GlasbeyARGBLut;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.serialize.display.VisibilityListener;
import org.embl.mobie.lib.source.AnnotationType;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerPanel;
import bvb.core.BigVolumeBrowser;
import bvvpg.vistools.BvvStackSource;
import ij.IJ;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;


@SuppressWarnings( "rawtypes" )
public class BigVolumeBrowserMoBIE implements ColoringListener, SelectionListener, TimePointListener, BigVolumeBrowser.Listener
{
	private BigVolumeBrowser bvb = null;
	
	private final ConcurrentHashMap< SourceAndConverter<?>, ValuePair< BvvStackSource<?>, AbstractSpimData<?>> > sacToBvvSource;
	
	private List< VisibilityListener > listeners = new ArrayList<>(  );
	
	public BigVolumeBrowserMoBIE()
	{
		sacToBvvSource = new ConcurrentHashMap<>();
	}
	
	public synchronized void init()
	{
		if (bvb == null)
		{
			bvb = new BigVolumeBrowser();
			bvb.startBVB("MoBIE BigVolumeBrowser");
			final Actions actions = new Actions( new InputTriggerConfig() ); 
			actions.runnableAction(
					() -> {	syncViewWithSliceViewer();},
					"sync with sliceViewer",
					"D" );
			actions.install( bvb.bvvHandle.getKeybindings(), "mobie-bvv-actions" );
			
			bvb.bvvViewer.addTimePointListener( this );
			
			bvb.controlPanel.cpFrame.addWindowListener(  
					new WindowAdapter()
					{
						@Override
						public void windowClosing( WindowEvent ev )
						{
							bvb = null;
							sacToBvvSource.clear();
							for ( VisibilityListener listener : listeners )
							{
								listener.visibility( false );
							}
							listeners.clear();
						}
					});
			bvb.addBVBListener( this );
		}
	
	}	
	
	public void showSource( SourceAndConverter< ? > sac, boolean isVisible )
	{
		if ( isVisible && bvb == null )
		{
			init();
		}
		if ( sacToBvvSource.containsKey( sac ) )
		{
			sacToBvvSource.get( sac ).getA().setActive( isVisible );
		}
		else
		{
			if ( isVisible )
			{
				addSourceToBVB( sac );
			}
		}
	}
	
	void addSourceToBVB( SourceAndConverter< ? > sac )
	{
		System.out.println( "BigVolumeViewer: " + sac.getSpimSource().getName() + ": " + sac );

		Source< ? > source = getSource( sac );
		final ValuePair< AbstractSpimData< ? >, List< BvvStackSource< ? > > > outPair = bvb.addSource( source, bvb.dataTreeModel.getIconMoBIE() );
		final AbstractSpimData< ? > spimData = outPair.getA();
		
		if( spimData == null )
		{
			RandomAccess< ? > randomAccess = source.getSource( 0, 0 ).randomAccess();
			IJ.log( "Cannot display " + source.getName() + " in BVV, incompatible data type:\n" +
					randomAccess.get().getClass().getName() );
			return;
		}

		//assume it is always one source
		final BvvStackSource< ? > bvvSource = outPair.getB().get( 0 );
		sacToBvvSource.put( sac, new ValuePair< >( outPair.getB().get( 0 ), outPair.getA()));

		configureRenderingSettings( sac, bvvSource );
	}
	
	
	private static Source< ? > getSource( SourceAndConverter< ? > sac )
	{
		Image< ? > image = DataStore.getImage( sac.getSpimSource().getName() );
		
		if ( image instanceof AnnotatedLabelImage )
		{
			return  ( ( AnnotatedLabelImage<?> ) image ).getLabelImage().getSourcePair().getSource();
		}
		return sac.getSpimSource();
	}
	
	private void configureRenderingSettings(
			SourceAndConverter< ? > sac,
			BvvStackSource< ? > bvvSource )
	{
		
		double displayRangeMin = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMin();
		double displayRangeMax = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMax();
		
		//render everything as volumetric
		bvvSource.setRenderType( 1 );
		
		if( isAnnotation( sac ) )
		{
			
			final IndexColorModel icmAnnLUT = getAnnotationLUT( sac );
			bvvSource.setLUT( icmAnnLUT,Integer.toString( icmAnnLUT.hashCode() ) );
			bvvSource.setDisplayRangeBounds( 0, icmAnnLUT.getMapSize() - 1 );
			bvvSource.setDisplayRange( 0, icmAnnLUT.getMapSize() - 1 );
			bvvSource.setAlphaRangeBounds( 0, 1 );
			bvvSource.setAlphaRange( 0, 1 );
			bvvSource.setVoxelRenderInterpolation( 0 );
		}
		else
		{
			final ARGBType color = ( ( ColorConverter ) sac.getConverter() ).getColor();

			bvvSource.setColor( color );
			final Object type = sac.getSpimSource().getSource( 0, 0 ).getType();
			final double[] contrastLimits = new double[ 2 ];
			contrastLimits[ 0 ] = 0;
			if ( type instanceof UnsignedByteType )
				contrastLimits[ 1 ] = 255;
			else
				contrastLimits[ 1 ] = 65535;
			
			if(type instanceof FloatType || type instanceof DoubleType)
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
		return DataStore.getImage( sac.getSpimSource().getName() ) instanceof AnnotatedLabelImage;
	}
	
	/** returns RGB LUT from the annotation image, i.e. UnsignedLongType
	 * value to RGB. The size of LUT is #of labels + 1, 
	 * since it adds Color.BLACK as zero values. 
	 * Works only if the number of labels is <=65535 **/
	
	@SuppressWarnings( { "unchecked"} )
	public IndexColorModel getAnnotationLUT( SourceAndConverter< ? > sac )
	{
		Image< ? > image = DataStore.getImage( sac.getSpimSource().getName() );
		if ( image instanceof AnnotatedLabelImage )
		{
			AnnotationAdapter< Annotation > annotationAdapter = ( ( AnnotatedLabelImage< Annotation > ) image ).getAnnotationAdapter();
			Converter< AnnotationType<?>, ARGBType > converter = ( Converter< AnnotationType<?>, ARGBType > ) sac.getConverter();
			String imageName = image.getName();
			int timePoint = 0;

			int numColors;
			int maxNumColors = 65535 - 1;
			if ( annotationAdapter instanceof LazyAnnotationAdapter )
			{
				numColors = maxNumColors;
			}
			else if( bvb.bvvViewer.state().getNumTimepoints() > 1 )
			{
				timePoint = bvb.bvvViewer.state().getCurrentTimepoint();
				numColors = numberOfAnnotationsPerTimepoint(annotationAdapter, timePoint, imageName);
			}
			else
			{
				numColors = ( ( AnnotatedLabelImage<?> ) image ).getAnnData().getTable().numAnnotations();
			}

			if ( numColors > maxNumColors )
			{
				IJ.log( "[WARN] There are more than 65535 annotations; coloring in BVV will be compromised." );
				numColors = maxNumColors;
			}
			
			final byte [][] colors = new byte [3][numColors+1];
			final byte [] alphas = new byte [numColors+1];
			ARGBType valARGB = new ARGBType();
			int val;
			
			// 0 is background is black
			colors[0][0] = 0;
			colors[1][0] = 0;
			colors[2][0] = 0;
			alphas[0] = ( byte ) ( 0 );
			
			for(int label=1; label<=numColors; label++)
			{
				final Annotation annotation = annotationAdapter.getAnnotation( imageName, timePoint, label );

				converter.convert( new AnnotationType<>( annotation ), valARGB );
				val = valARGB.get();
				colors[ 0 ][ label ] = ( byte ) ARGBType.red( val );
				colors[ 1 ][ label ] = ( byte ) ARGBType.green( val );
				colors[ 2 ][ label ] = ( byte ) ARGBType.blue( val );
				alphas[ label ] = (byte) ARGBType.alpha( val );
			}
			return new IndexColorModel(16,numColors+1,colors[0],colors[1],colors[2], alphas);
		}
		return null;
	}
	
	static int numberOfAnnotationsPerTimepoint(final AnnotationAdapter< Annotation > annotationAdapter, final int timePoint, String imageName)
	{
		int n = 1;
		while(annotationAdapter.getAnnotation( imageName, timePoint, n ) != null)
		{
			n++;
		}
		return n-1;
	}
	
	public synchronized BigVolumeBrowser getBVB()
	{
		return bvb;
	}
	
	public void close()
	{
		if ( bvb != null )
		{
			bvb.shutDownAll();
		}
	}
	
	public void removeSources(List< ? extends SourceAndConverter< ? > > sourceAndConverters)
	{
		for ( SourceAndConverter< ? > sac : sourceAndConverters )
		{
			if(sacToBvvSource.containsKey( sac ))
			{
				sacToBvvSource.get( sac ).getA().removeFromBdv();
				sacToBvvSource.remove( sac );
			}
		}
	
	}
	
	public Collection< VisibilityListener > getListeners()
	{
		return listeners;
	}

	
	void syncViewWithSliceViewer()
	{
		if(bvb != null)
		{
			ViewerPanel bdvViewer = MoBIE.getInstance().getViewManager().getSliceViewer().getBdvHandle().getViewerPanel();
			AffineTransform3D transform = bdvViewer.state().getViewerTransform();
			Dimension bdvDim = bdvViewer.getSize();
			Dimension bvvDim = bvb.bvvViewer.getSize();
			transform.set( transform.get( 0, 3 ) - bdvDim.width / 2, 0, 3 );
			transform.set( transform.get( 1, 3 ) - bdvDim.height / 2, 1, 3 );
			transform.scale( 1.0/ bdvDim.width );
			transform.scale( bvvDim.width );
			transform.set( transform.get( 0, 3 ) + bvvDim.width / 2, 0, 3 );
			transform.set( transform.get( 1, 3 ) + bvvDim.height / 2, 1, 3 );
			
			bvb.bvvViewer.state().setViewerTransform( transform );
			bvb.bvvViewer.state().
					setCurrentTimepoint(bdvViewer.state().getCurrentTimepoint());
		}
	}

	@Override
	public void selectionChanged()
	{
		updateAnnotations();		
	}
	
	@Override
	public void focusEvent( Object selection, Object initiator )
	{
		
	}
	
	@Override
	public void coloringChanged()
	{
		updateAnnotations();		
	}
	
	@Override
	public void timePointChanged( int timePointIndex )
	{
		updateAnnotations();		
	}
	
	public void updateAnnotations()	
	{
		if(bvb != null)
		{
			for ( Map.Entry< SourceAndConverter<?>, ValuePair< BvvStackSource<?>, AbstractSpimData<?>> > entry : sacToBvvSource.entrySet() )
			{
				if ( isAnnotation( entry.getKey() ) )
					configureRenderingSettings( entry.getKey(), entry.getValue().getA() );
			}
		}		
		
	}

	@Override
	public void bvbRestarted()
	{
		//update the map
		//first store sac and spimdata
		ArrayList<ValuePair<SourceAndConverter<?>,AbstractSpimData<?>>> sacSpimList = new ArrayList<>();
		
		for (Map.Entry<SourceAndConverter<?>, ValuePair< BvvStackSource<?>, AbstractSpimData<?>> > entry : sacToBvvSource.entrySet()) 
		{
			sacSpimList.add( new ValuePair< >(entry.getKey(),entry.getValue().getB()) );			
		}
		//update map
		sacToBvvSource.clear();
		for(int i=0; i<sacSpimList.size(); i++)
		{
			final AbstractSpimData<?> spimData = sacSpimList.get( i ).getB();
			sacToBvvSource.put( sacSpimList.get( i ).getA(), 
					new ValuePair< >(bvb.getBVVSourcesList( spimData ).get( 0 ),
					spimData) );
		}
	}
	
	/** leftover example of Glasbey LUT, keep it for now.**/
	public static IndexColorModel getGlasbeyICM()
	{
		final GlasbeyARGBLut gARGB = new GlasbeyARGBLut();
		final byte [][] colors = new byte [3][256];
		int val;
		for(int i=0;i<256;i++)
		{
			val = gARGB.getARGB( i );
			colors[0][i] = ( byte ) ARGBType.red( val );
			colors[1][i] = ( byte ) ARGBType.green( val );
			colors[2][i] = ( byte ) ARGBType.blue( val );
		}

		return new IndexColorModel(16,256,colors[0],colors[1],colors[2]);
	}
}
