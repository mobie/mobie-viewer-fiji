package org.embl.mobie.viewer.command;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.color.LabelConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Change Random Coloring Seed [ Ctrl L ]")
public class RandomColorSeedChangerCommand implements BdvPlaygroundActionCommand
{
	// TODO: Maybe implement a setter (the Categorical coloring models have the method for this already)
	//@Parameter( label = "Random color seed" )
	//int seed = 42;

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

				converter.getColoringModel().incrementRandomColorSeed();
			}
		}
	}
}
