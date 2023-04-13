package projects.kristina.cmd;

import org.embl.mobie.cmd.TableCmd;

class KristinaOpenCellProfilerImageTableCmd
{
	public static void main( String[] args ) throws Exception
	{
		final TableCmd cmd = new TableCmd();
		cmd.root = "/Volumes/cba/exchange/kristina-mirkes/data/processed";
		cmd.table = "/Volumes/cba/exchange/kristina-mirkes/data/processed/Image.txt";
		cmd.images = new String[]{"RawData=FileName_RawData","WormProb=FileName_WormProb"};
		cmd.removeSpatialCalibration = true;
		cmd.call();
	}
}