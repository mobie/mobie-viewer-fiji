import de.embl.cba.platynereis.platybrowser.PlatyBrowserCommand;
import net.imagej.ImageJ;

public class RunExplorePlatynereisAtlasCommand
{

	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( PlatyBrowserCommand.class, true );
	}

}
