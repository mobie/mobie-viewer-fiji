package develop;

import org.embl.mobie.cmd.MoBIECmd;

public class OpenFromTable
{
	public static void main( String[] args ) throws Exception
	{
		final MoBIECmd cmd = new MoBIECmd();
		cmd.table = "/Users/tischer/Desktop/Kristina/big-table-merge/table.csv";
		cmd.root = "/Users/tischer/Desktop/Kristina/big-table";
		cmd.images = new String[]{"worms=ImagePath_ProcessedMovie"};
		cmd.labels = new String[]{"segmentation=ImagePath_LabelMask"};
		cmd.call();
	}
}
