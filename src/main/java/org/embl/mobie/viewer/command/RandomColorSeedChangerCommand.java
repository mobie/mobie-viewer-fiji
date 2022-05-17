package org.embl.mobie.viewer.command;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import ij.IJ;
import org.embl.mobie.viewer.color.LabelConverter;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Change Random Coloring Seed [ Ctrl L ]")
public class RandomColorSeedChangerCommand implements BdvPlaygroundActionCommand
{
	@Parameter( label = "Increment random color seed [ Ctrl L ]", callback = "incrementRandomColorSeed" )
	Button button;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Override
	public void run()
	{
		incrementRandomColorSeed( sourceAndConverters );
	}

	public static void incrementRandomColorSeed( SourceAndConverter[] sourceAndConverters )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof LabelConverter< ? > )
			{
				final LabelConverter< ? > converter = ( LabelConverter< ? > ) sourceAndConverter.getConverter();

				final ColoringModel< ? > coloringModel = converter.getColoringModel().getWrappedColoringModel();
				if ( coloringModel instanceof CategoryColoringModel )
				{
					int randomSeed = ( ( CategoryColoringModel< ? > ) coloringModel ).getRandomSeed();
					( ( CategoryColoringModel<?> ) coloringModel ).setRandomSeed( ++randomSeed );
				}
			}
		}
	}
}
