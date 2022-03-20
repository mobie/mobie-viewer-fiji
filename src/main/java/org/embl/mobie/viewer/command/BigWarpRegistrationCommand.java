package org.embl.mobie.viewer.command;

import bdv.gui.TransformTypeSelectDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bigwarp.BigWarp;
import bigwarp.transforms.BigWarpTransform;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import org.embl.mobie.viewer.transform.TransformHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - 3D BigWarp")
public class BigWarpRegistrationCommand implements BdvPlaygroundActionCommand, TransformListener< InvertibleRealTransform >
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter(label = "Fixed Source(s)")
	SourceAndConverter[] fixedSources;

	@Parameter(label = "Moving Source(s)")
	SourceAndConverter[] movingSources;

	private BigWarp bigWarp;
	private Map< SourceAndConverter< ? >, AffineTransform3D > sacToOriginalFixedTransform;
	private List< SourceAndConverter > movingSacs;
	private List< SourceAndConverter > fixedSacs;

	private ISourceAndConverterService sacService;
	private SourceAndConverterBdvDisplayService bdvDisplayService;

	@Override
	public void run()
	{
		// FIXME put all of this into a new thread?

		sacService = SourceAndConverterServices.getSourceAndConverterService();
		bdvDisplayService = SourceAndConverterServices.getBdvDisplayService();

		movingSacs = Arrays.stream( movingSources ).collect( Collectors.toList() );
		fixedSacs = Arrays.stream( fixedSources ).collect( Collectors.toList() );

		storeOriginalTransforms( movingSacs );

		List< ConverterSetup > converterSetups = Arrays.stream( movingSources ).map( src -> sacService.getConverterSetup(src)).collect( Collectors.toList() );
		converterSetups.addAll( Arrays.stream( fixedSources ).map( src -> sacService.getConverterSetup( src) ).collect( Collectors.toList() ) );

		BigWarpLauncher bigWarpLauncher = new BigWarpLauncher( movingSacs, fixedSacs, "Big Warp", converterSetups);
		bigWarpLauncher.run();

		bdvDisplayService.pairClosing( bigWarpLauncher.getBdvHandleQ(), bigWarpLauncher.getBdvHandleP() );

		bigWarp = bigWarpLauncher.getBigWarp();

		final AffineTransform3D normalisedViewerTransform = TransformHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );
		applyViewerTransform( normalisedViewerTransform, bigWarp.getViewerFrameQ().getViewerPanel() );
		applyViewerTransform( normalisedViewerTransform, bigWarp.getViewerFrameP().getViewerPanel() );
		bigWarp.setTransformType( TransformTypeSelectDialog.AFFINE );
		bigWarp.addTransformListener( this );

		new Thread( () -> showDialog() ).start();
	}

	private void applyViewerTransform( AffineTransform3D normalisedViewerTransform, BigWarpViewerPanel viewerPanel )
	{
		viewerPanel.state().setViewerTransform( TransformHelper.createUnnormalizedViewerTransform( normalisedViewerTransform, viewerPanel ) );
	}

	private void showDialog()
	{
		final NonBlockingGenericDialog dialog = new NonBlockingGenericDialog( "Registration - BigWarp" );
		dialog.addMessage( "Landmark based affine, similarity, rigid and translation transformations.\nPlease read the BigWarp help.\n" + "Press [ OK ] to close BigWarp and apply the current registration in MoBIE.");
		dialog.showDialog();
		if ( dialog.wasCanceled() )
		{
			resetMovingTransforms();
			bigWarp.closeAll();
		}
		else
		{
			setMovingTransforms();
			bigWarp.closeAll();
		}
	}

	private void resetMovingTransforms()
	{
		for ( SourceAndConverter movingSac : movingSacs )
		{
			( ( TransformedSource) movingSac.getSpimSource() ).setFixedTransform( sacToOriginalFixedTransform.get( movingSac ) );
		}
		bdvHandle.getViewerPanel().requestRepaint();
	}

	private void storeOriginalTransforms( List< SourceAndConverter > movingSacs )
	{
		sacToOriginalFixedTransform = new HashMap<>();
		for ( SourceAndConverter movingSac : movingSacs )
		{
			final AffineTransform3D fixedTransform = new AffineTransform3D();
			( ( TransformedSource ) movingSac.getSpimSource()).getFixedTransform( fixedTransform );
			sacToOriginalFixedTransform.put( movingSac, fixedTransform );
		}
	}

	@Override
	public void transformChanged( InvertibleRealTransform transform )
	{
		final String transformType = bigWarp.getTransformType();
		if ( transformType.equals( TransformTypeSelectDialog.TPS ) )
		{
			System.err.println( TransformTypeSelectDialog.TPS + "is currently not supported by MoBIE please choose any of the other transform types by selecting one of the BigWarp windows and pressing F2.");
		}
		else
		{
			//setMovingTransforms();
		}
	}

	private void setMovingTransforms()
	{
		final BigWarpTransform bwTransform = bigWarp.getBwTransform();
		final AffineTransform3D bwAffineTransform = bwTransform.affine3d();
		for ( SourceAndConverter< ? > movingSource : movingSources )
		{
			final AffineTransform3D combinedTransform = sacToOriginalFixedTransform.get( movingSource ).copy();
			combinedTransform.preConcatenate( bwAffineTransform.copy().inverse() );
			final TransformedSource< ? > transformedSource = ( TransformedSource< ? > ) movingSource.getSpimSource();
			transformedSource.setFixedTransform( combinedTransform );
		}
		bdvHandle.getViewerPanel().requestRepaint();
	}
}
