package de.embl.cba.mobie.color;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Change opacity of non-selected segments")
public class NonSelectedSegmentsOpacityAdjusterCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	BdvHandle bdvh;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Opacity of non-selected segments" )
	double opacity = 0.15;

	@Override
	public void run()
	{
		setNonSelectedSegmentsOpacity( bdvh, sourceAndConverters, opacity );
	}

	public static void setNonSelectedSegmentsOpacity( BdvHandle bdvh, SourceAndConverter[] sourceAndConverters, double opacity )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof LabelConverter< ? > )
			{
				final LabelConverter< ? > converter = ( LabelConverter< ? > ) sourceAndConverter.getConverter();

				converter.getColoringModel().setOpacityNotSelected( opacity );
			}
		}

		bdvh.getViewerPanel().requestRepaint();
	}
}
