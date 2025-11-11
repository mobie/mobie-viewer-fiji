package org.embl.mobie.lib.bvv;

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
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;

import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.*;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.lut.GlasbeyARGBLut;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.serialize.display.VisibilityListener;
import org.embl.mobie.lib.source.AnnotationType;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerPanel;
import bvvpg.core.VolumeViewerFrame;
import bvvpg.pguitools.GammaConverterSetup;
import bvvpg.vistools.Bvv;
import bvvpg.vistools.BvvFunctions;
import bvvpg.vistools.BvvHandleFrame;
import bvvpg.vistools.BvvStackSource;
import ij.IJ;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.services.SourceAndConverterServices;


@SuppressWarnings( "rawtypes" )
public class BigVolumeViewerMoBIE implements ColoringListener, SelectionListener, TimePointListener
{
	private Bvv bvv = null;
	
	public BvvHandleFrame handle = null;
	
	private final ConcurrentHashMap< SourceAndConverter, ValuePair< BvvStackSource, AbstractSpimData> > sacToBvvSource;
	
	private final List< VisibilityListener > listeners = new ArrayList<>(  );
	
	private int nRenderMethod = 1;
	
	volatile boolean bRestarting = false;
	
	public BigVolumeViewerMoBIE()
	{
		//sourceAndConverters = new ArrayList<>();
		sacToBvvSource = new ConcurrentHashMap<>();
		BvvSettings.readBVVRenderSettings();
	}
	
	public synchronized void init()
	{
		if ( bvv == null )
		{
			bvv = BvvFunctions.show( Bvv.options().frameTitle( "MoBIE BigVolumeViewer" ).
					dCam(BvvSettings.dCam).
					dClipNear(BvvSettings.dClipNear).
					dClipFar(BvvSettings.dClipFar).				
					renderWidth(BvvSettings.renderWidth).
					renderHeight(BvvSettings.renderHeight).
					numDitherSamples(BvvSettings.numDitherSamples ).
					cacheBlockSize(BvvSettings.cacheBlockSize ).
					maxCacheSizeInMB( BvvSettings.maxCacheSizeInMB ).
					ditherWidth(BvvSettings.ditherWidth)
					);
			
			handle = (BvvHandleFrame)bvv.getBvvHandle();
			
			//change drag rotation for navigation "3D Viewer" style
			final Rotate3DViewerStyle dragRotate = new Rotate3DViewerStyle( 0.75, handle);
			final Rotate3DViewerStyle dragRotateFast = new Rotate3DViewerStyle( 2.0, handle);
			final Rotate3DViewerStyle dragRotateSlow = new Rotate3DViewerStyle( 0.1, handle);
			
			final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
			behaviours.behaviour( dragRotate, "drag rotate", "button1" );
			behaviours.behaviour( dragRotateFast, "drag rotate fast", "shift button1" );
			behaviours.behaviour( dragRotateSlow, "drag rotate slow", "ctrl button1" );
			behaviours.install( handle.getTriggerbindings(), "mobie-bvv-behaviours" );
			
			final Actions actions = new Actions( new InputTriggerConfig() ); 
			actions.runnableAction(
					() -> {	syncViewWithSliceViewer();},
					"sync with sliceViewer",
					"D" );
			actions.install( handle.getKeybindings(), "mobie-bvv-actions" );
			
			handle.getViewerPanel().addTimePointListener( this );
			
			handle.getBigVolumeViewer().getViewerFrame().addWindowListener(  
					new WindowAdapter()
					{
						@Override
						public void windowClosing( WindowEvent ev )
						{
							bvv = null;
							sacToBvvSource.clear();
							handle = null;
							if(!bRestarting)
							{
								for ( VisibilityListener listener : listeners )
								{
									listener.visibility( false );
								}
							}							
						}
					});
		}
	
	}

	public void showSource( SourceAndConverter< ? > sac, boolean isVisible )
	{
		if ( isVisible && bvv == null )
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
				addSourceToBVV( sac );
			}
		}
	}
	
	void addSourceToBVV( SourceAndConverter< ? > sac )
	{
		System.out.println( "BigVolumeViewer: " + sac.getSpimSource().getName() + ": " + sac );

		Source< ? > source = getSource( sac );

		final AbstractSpimData< ? > spimData = SourceToSpimDataWrapper.wrap( source );

//		BvvFunctions.show( source );
//		BdvFunctions.show( spimData );
		
		if( spimData == null )
		{
			RandomAccess< ? > randomAccess = source.getSource( 0, 0 ).randomAccess();
			IJ.log( "Cannot display " + source.getName() + " in BVV, incompatible data type:\n" +
					randomAccess.get().getClass().getName() );
			return;
		}

		nRenderMethod = 1;
		
		//in not a first source, ensure consistent rendering of all sources
		if(	handle.getViewerPanel().state().getSources().size()>0)
		{
			@SuppressWarnings( "deprecation" )
			GammaConverterSetup gConvSetup = ((GammaConverterSetup)handle.getSetupAssignments().getConverterSetups().get( 0 ));	
			nRenderMethod = gConvSetup.getRenderType();
		}
		
		//assume it is always one source
		BvvStackSource< ? >  bvvSource = BvvFunctions.show(spimData,
				Bvv.options().addTo( bvv )).get( 0 );
		sacToBvvSource.put( sac, new ValuePair< >( bvvSource, spimData));

		configureRenderingSettings( sac, bvvSource );
	}
	
	private static Source< ? > getSource( SourceAndConverter< ? > sac )
	{
		Image< ? > image = DataStore.getImage( sac.getSpimSource().getName() );
		
		if ( image instanceof AnnotatedLabelImage )
		{
			return  ( ( AnnotatedLabelImage<?> ) image ).getLabelImage().getSourcePair().getSource();
		}
		else
		{
			return sac.getSpimSource();
		}
	}
	
	private void configureRenderingSettings(
			SourceAndConverter< ? > sac,
			BvvStackSource< ? > bvvSource )
	{
		bvvSource.setRenderType( nRenderMethod );
		double displayRangeMin = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMin();
		double displayRangeMax = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac ).getDisplayRangeMax();
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
			final Object type = Util.getTypeFromInterval( sac.getSpimSource().getSource( 0, 0 ) );
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
	 * Works only if the number of labels is smaller or equal to 65535 **/
	
	@SuppressWarnings( { "unchecked"} )
	public IndexColorModel getAnnotationLUT( SourceAndConverter< ? > sac )
	{
		Image< ? > image = DataStore.getImage( sac.getSpimSource().getName() );
		if ( image instanceof AnnotatedLabelImage )
		{
			AnnotationAdapter< Annotation > annotationAdapter = ( ( AnnotatedLabelImage< Annotation > ) image ).getAnnotationAdapter();
			Converter< AnnotationType, ARGBType > converter = ( Converter< AnnotationType, ARGBType > ) sac.getConverter();
			String imageName = image.getName();
			int timePoint = 0;

			int numColors;
			int maxNumColors = 65535 - 1;
			if ( annotationAdapter instanceof LazyAnnotationAdapter )
			{
				numColors = maxNumColors;
			}
			else if( handle.getViewerPanel().state().getNumTimepoints() > 1 )
			{
				timePoint = handle.getViewerPanel().state().getCurrentTimepoint();
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
	
	public synchronized Bvv getBVV()
	{
		return bvv;
	}
	
	public void close()
	{
		if ( bvv != null )
		{
			bvv.close();
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
	
	public void updateBVVRenderSettings()
	{
		boolean bRestartBVV = BvvSettings.readBVVRenderSettings();
		if (bvv != null)
		{
			if(!bRestartBVV)
			{
				handle.getViewerPanel().setCamParams( BvvSettings.dCam, BvvSettings.dClipNear, BvvSettings.dClipFar );
				handle.getViewerPanel().requestRepaint();
			}
			else
			{
				restartBVV();
			}
		}
	}
	
	void syncViewWithSliceViewer()
	{
		if(bvv != null)
		{
			ViewerPanel bdvViewer = MoBIE.getInstance().getViewManager().getSliceViewer().getBdvHandle().getViewerPanel();
			AffineTransform3D transform = bdvViewer.state().getViewerTransform();
			Dimension bdvDim = bdvViewer.getSize();
			Dimension bvvDim = handle.getViewerPanel().getSize();
			transform.set( transform.get( 0, 3 ) - bdvDim.width / 2, 0, 3 );
			transform.set( transform.get( 1, 3 ) - bdvDim.height / 2, 1, 3 );
			transform.scale( 1.0/ bdvDim.width );
			transform.scale( bvvDim.width );
			transform.set( transform.get( 0, 3 ) + bvvDim.width / 2, 0, 3 );
			transform.set( transform.get( 1, 3 ) + bvvDim.height / 2, 1, 3 );
			
			handle.getViewerPanel().state().setViewerTransform( transform );
			handle.getViewerPanel().state().
					setCurrentTimepoint(bdvViewer.state().getCurrentTimepoint());
		}
	}

	void restartBVV()
	{
		IJ.log( "Restarting BigVolumeViewer..." );
		//gather all the sources
		ArrayList<BvvSourceStateMobie> sourceStates = new ArrayList<>();
		for ( Map.Entry< SourceAndConverter, ValuePair< BvvStackSource, AbstractSpimData> > entry : sacToBvvSource.entrySet() )
		{
			sourceStates.add( new BvvSourceStateMobie(entry.getKey(),
					entry.getValue().getB(),
					handle.getViewerPanel().state().isSourceVisible( ( SourceAndConverter< ? > ) entry.getValue().getA().getSources().get( 0 ) )
					) );
		}
		//save window position and size on the screen
		VolumeViewerFrame bvvFrame = handle.getBigVolumeViewer().getViewerFrame();
	    final java.awt.Point bvv_p = bvvFrame.getLocationOnScreen();
	    final java.awt.Dimension bvv_d = bvvFrame.getSize();
		//let's save viewer transform
		AffineTransform3D viewTransform = handle.getViewerPanel().state().getViewerTransform();


		//now restart
		bRestarting = true;
		close();
		init();
		bRestarting = false;
		
		//restore window position
		bvvFrame = handle.getBigVolumeViewer().getViewerFrame();
		bvvFrame.setLocation( bvv_p );
		bvvFrame.setPreferredSize( bvv_d );	
		bvvFrame.pack();
		
		//put back sources
		for(BvvSourceStateMobie state:sourceStates)
		{
			BvvStackSource< ? >  bvvSource = BvvFunctions.show(state.spimData,
					Bvv.options().addTo( bvv )).get( 0 );
			sacToBvvSource.put( state.sac, new ValuePair<>( bvvSource, state.spimData));

			configureRenderingSettings( state.sac, bvvSource );
			bvvSource.setActive( state.bVisible );
		}
		//put back viewer transform
		handle.getViewerPanel().state().setViewerTransform( viewTransform );
			
		IJ.log( "..done." );
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
	
	public void updateAnnotations()
	{
		if(bvv != null)
		{
			for ( Map.Entry< SourceAndConverter, ValuePair< BvvStackSource, AbstractSpimData> > entry : sacToBvvSource.entrySet() )
			{
				if ( isAnnotation( entry.getKey() ) )
					configureRenderingSettings( entry.getKey(), entry.getValue().getA() );
			}
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

	@Override
	public void timePointChanged( int timePointIndex )
	{
		updateAnnotations();
	}
}
