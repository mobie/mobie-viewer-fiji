import de.embl.cba.platynereis.platybrowser.ExplorePlatynereisAtlasCommand;
import de.embl.cba.platynereis.ui.PlatyBrowserCommand;
import net.imagej.ImageJ;

public class RunExplorePlatynereisAtlasCommand
{

	public static void main(final String... args) throws Exception
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( ExplorePlatynereisAtlasCommand.class, true );
	}

}
