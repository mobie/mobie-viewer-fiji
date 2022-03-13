package org.embl.mobie.viewer.command;

import bdv.gui.TransformTypeSelectDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bigwarp.BigWarp;
import bigwarp.transforms.BigWarpTransform;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - BigWarp")
public class BigWarpRegistrationCommand implements BdvPlaygroundActionCommand, TransformListener< InvertibleRealTransform >
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter(label = "Moving Source(s)")
	SourceAndConverter[] movingSources;

	@Parameter(label = "Fixed Source(s)")
	SourceAndConverter[] fixedSources;

	private BigWarp bigWarp;
	private Map< SourceAndConverter< ? >, AffineTransform3D > sacToOriginalFixedTransform;
	private List< SourceAndConverter > movingSacs;
	private List< SourceAndConverter > fixedSacs;

	private ISourceAndConverterService sacService;
	private SourceAndConverterBdvDisplayService bdvDisplayService;

	@Override
	public void run()
	{
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
		bigWarp.getViewerFrameQ().getViewerPanel().state().setViewerTransform( bdvHandle.getViewerPanel().state().getViewerTransform() );
		bigWarp.getViewerFrameP().getViewerPanel().state().setViewerTransform( bdvHandle.getViewerPanel().state().getViewerTransform() );
		bigWarp.setTransformType( TransformTypeSelectDialog.AFFINE );
		bigWarp.addTransformListener( this );

		new Thread( () -> showDialog() ).start();
	}

	private void showDialog()
	{
		final NonBlockingGenericDialog dialog = new NonBlockingGenericDialog( "Registration - BigWarp" );
		dialog.addMessage( "Landmark based affine, similarity, rigid and translation transformations.\n\nPlease read the BigWarp help.\n" +
				"The transformation will be applied in MoBIE immediately.\n" +
				"Press [ Cancel ] to revert the changes and close BigWarp.\n" +
				"Press [ OK ] to close BigWarp and keep the changes.");
		dialog.showDialog();
		if ( dialog.wasCanceled() )
		{
			resetMovingTransforms();
			bigWarp.closeAll();
		}
		else
		{
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
			final BigWarpTransform bwTransform = bigWarp.getBwTransform();
			final AffineTransform3D affineTransform3D = bwTransform.affine3d();
			for ( SourceAndConverter< ? > movingSource : movingSources )
			{
				(( TransformedSource< ? > ) movingSource.getSpimSource()).setFixedTransform( affineTransform3D );
			}
			bdvHandle.getViewerPanel().requestRepaint();
		}
	}
}
