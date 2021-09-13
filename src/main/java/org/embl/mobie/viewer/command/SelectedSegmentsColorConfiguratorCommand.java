package org.embl.mobie.viewer.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.color.LabelConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Configure coloring of selected segments")
public class SelectedSegmentsColorConfiguratorCommand implements BdvPlaygroundActionCommand
{
	public static final String DEFAULT_COLOR = "Default color";
	public static final String SELECTED_COLOR = "Selected color";

	@Parameter
	BdvHandle bdvh;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Coloring mode", choices = { DEFAULT_COLOR, SELECTED_COLOR } )
	String coloringMode = DEFAULT_COLOR;

	@Parameter ( label = "Selected color")
	ColorRGB color = new ColorRGB(255,255,255);

	@Override
	public void run()
	{
		if ( coloringMode.equals( DEFAULT_COLOR ) )
		{
			setSelectedSegmentsColor( bdvh, sourceAndConverters, null );
		}
		else
		{
			final ARGBType argbType = new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
			setSelectedSegmentsColor( bdvh, sourceAndConverters, argbType );
		}
	}

	public static void setSelectedSegmentsColor( BdvHandle bdvh, SourceAndConverter[] sourceAndConverters, ARGBType argbType )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof LabelConverter< ? > )
			{
				final LabelConverter< ? > converter = ( LabelConverter< ? > ) sourceAndConverter.getConverter();

				converter.getColoringModel().setSelectionColor( argbType );
			}
		}

		bdvh.getViewerPanel().requestRepaint();
	}
}
