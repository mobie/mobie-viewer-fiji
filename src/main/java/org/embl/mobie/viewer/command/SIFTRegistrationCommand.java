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

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - SIFT")
public class SIFTRegistrationCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter(label = "Fixed Source")
	SourceAndConverter fixedSource;

	@Parameter(label = "Moving Source")
	SourceAndConverter movingSource;

	@Override
	public void run()
	{
		int a = 1;
	}

}
