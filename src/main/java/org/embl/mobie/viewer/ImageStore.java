package org.embl.mobie.viewer;

import org.embl.mobie.viewer.source.Image;

import java.util.HashMap;
import java.util.Map;

public abstract class ImageStore
{
	public static Map< String, Image< ? > > images = new HashMap<>();
}
