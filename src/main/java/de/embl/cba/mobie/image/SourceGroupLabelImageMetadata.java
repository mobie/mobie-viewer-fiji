package de.embl.cba.mobie.image;

import net.imglib2.RealInterval;

import java.util.Map;

public class SourceGroupLabelImageMetadata
{
	public Map< String, RealInterval > sourceNameToInterval;
	public Map< String, Integer > sourceNameToLabelIndex;
}
