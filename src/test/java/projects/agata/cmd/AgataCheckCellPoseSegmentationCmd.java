package projects.agata.cmd;

import org.embl.mobie.cmd.FilesCmd;

class AgataCheckCellPoseSegmentationCmd
{
	public static void main( String[] args ) throws Exception
	{
		final FilesCmd cmd = new FilesCmd();
		cmd.root = "/Volumes/cba/exchange/agata-misiaszek/data/analysed";
		cmd.images = new String[]{
				"nuclei=.*.ome.tif",
		} ;
		cmd.labels = new String[]{
				"labels=.*.ome_cp_masks.tif"
		} ;
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}