package develop;

import org.embl.mobie.cmd.ProjectCmd;

public class OpenFromTable
{
	public static void main( String[] args ) throws Exception
	{
		final ProjectCmd cmd = new ProjectCmd();
		cmd.table = "/Users/tischer/Desktop/Kristina/big-table-merge/table.csv";
		cmd.root = "/Users/tischer/Desktop/Kristina/big-table";
		cmd.images = new String[]{"worms=ImagePath_ProcessedMovie"};
		cmd.labels = new String[]{"worms_seg=ImagePath_LabelMask"};
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}
