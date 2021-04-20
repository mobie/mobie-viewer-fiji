package de.embl.cba.mobie2.select;

import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie2.color.LabelConverter;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import org.jetbrains.annotations.NotNull;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.List;
import java.util.function.Consumer;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Set Selected Segments Coloring Mode")
public class SelectionColoringModeChangerCommand implements BdvPlaygroundActionCommand
{
	// MoBIEColoringModel.SelectionColoringMode
	@Parameter( label = "Selected segments coloring mode", choices = { "SelectionColor", "DimNotSelected" } )
	String coloringMode = "DimNotSelected";

	@Parameter( label = "Opacity of non-selected segments" )
	double opacity = 0.15;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Override
	public void run()
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof LabelConverter< ? > )
			{
				final LabelConverter< ? > converter = ( LabelConverter< ? > ) sourceAndConverter.getConverter();

				converter.getColoringModel().setSelectionColoringMode( MoBIEColoringModel.SelectionColoringMode.valueOf( coloringMode ) );
				converter.getColoringModel().setOpacityNotSelected( opacity );
			}
		}
	}
}
