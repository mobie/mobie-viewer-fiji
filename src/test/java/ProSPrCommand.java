import de.embl.cba.platynereis.MainCommand;
import net.imagej.ImageJ;

public class PlatynereisCommand
{

	public static void main(final String... args) throws Exception
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// invoke the plugin
		ij.command().run( MainCommand.class, true );
	}

}
