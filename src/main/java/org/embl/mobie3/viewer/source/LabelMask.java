package org.embl.mobie3.viewer.source;

import net.imglib2.type.numeric.integer.IntType;

public class LabelMask< T extends IntType > implements Image< T >
{
	@Override
	public SourcePair< T > getSourcePair()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return null;
	}
}
