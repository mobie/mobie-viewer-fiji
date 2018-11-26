import de.embl.cba.platynereis.PlatyBrowser;
import de.embl.cba.platynereis.ui.PlatyBrowserCommand;
import net.imagej.ImageJ;

public class TestPlatyBrowserCommand
{

	public static void main(final String... args) throws Exception
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( PlatyBrowserCommand.class, true );
	}

}
