package projects.govendir;

import org.embl.mobie.cmd.MoBIECmd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MoBIECmdMattLocal
{
	public static void main( String[] args ) throws Exception
	{
		//  2023_01_18--Thrombin_30min_1_holes_seg.tif
		String testName = "2022_00_00__EGM_8hr_1--jun";
		// "(?<treat>"
		final String regex = "(?<date>.*)__(?<treat>.*_.*)_.*--jun";
		final Pattern pattern = Pattern.compile( regex );
		final Matcher matcher = pattern.matcher( testName );
		final boolean matches = matcher.matches();

		final MoBIECmd cmd = new MoBIECmd();
		cmd.images = new String[]{ "/Users/tischer/Desktop/matt/preprocessed/*--jun.tif" };
		//cmd.segmentations = new String[]{ "/Users/tischer/Desktop/matt/analysed/*_seg.tif" };
		//cmd.tables = new String[]{ "/Users/tischer/Desktop/matt/analysed/*_seg.csv" };
		// if it is like this "*_1*" one would expect 4 grids.
		// XYZ__EGM_8hr_1--jun.tif
		cmd.grids = new String[]{ "(?<date>*)__(?<treat>*_*)_*--jun" };
		cmd.call();
	}
}