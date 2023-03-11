package develop;

import ij.IJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

public class CommandTest implements Command
{
	@Parameter( label = "Choices", choices = { "A", "B" } )
	String choice;

	@Override
	public void run()
	{
		IJ.log("You chose " + choice);
	}
}
