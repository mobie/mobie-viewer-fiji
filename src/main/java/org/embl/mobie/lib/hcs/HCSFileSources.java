package org.embl.mobie.lib.hcs;

import org.embl.mobie.lib.io.IOHelper;

public class HCSFileSources
{
	public HCSFileSources( String hcsDirectory )
	{
		IOHelper.getPaths( hcsDirectory );

	}
}
