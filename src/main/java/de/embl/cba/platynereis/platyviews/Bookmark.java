package de.embl.cba.platynereis.platyviews;

import de.embl.cba.bdv.utils.sources.Metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Bookmark
{
	public String name;
	public ArrayList< Double > position = null;
	public ArrayList< Double > transform = null;
	public Map< String, Metadata > nameToMetadata = new HashMap<>();
}
