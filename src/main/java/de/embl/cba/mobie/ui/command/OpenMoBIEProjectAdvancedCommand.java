package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Expert>Open MoBIE Project Advanced..." )
public class OpenMoBIEProjectAdvancedCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String imagesLocation = "https://github.com/platybrowser/platybrowser";


//	@Parameter ( label = "Images Location" )
//	public String imagesLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Tables Location" )
	public String tablesLocation = "https://github.com/platybrowser/platybrowser";

	@Override
	public void run()
	{
		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				imagesLocation,
				tablesLocation );
	}

	public static void main(final String... args)
	{

//		new MoBIEViewer(  )
//	}
//		public MoBIEViewer(
//			String projectImagesLocation,
//			String projectTablesLocation,
//			MoBIEOptions options )
//		{


		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( OpenMoBIEProjectAdvancedCommand.class, true );
	}
}
