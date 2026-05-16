package org.embl.mobie.lib.serialize.transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialized transformation pointing to a precomputed displacement field (JSON + RAW).
 */
public class DisplacementFieldTransformation extends AbstractImageTransformation
{
	protected String displacementFieldUri;

	public DisplacementFieldTransformation(
			final String name,
			final String displacementFieldUri,
			final List< String > sources,
			final List< String > sourceNamesAfterTransform )
	{
		this.name = name;
		this.displacementFieldUri = displacementFieldUri;
		this.sources = sources;
		this.sourceNamesAfterTransform = sourceNamesAfterTransform;
	}

	public String getDisplacementFieldUri()
	{
		return displacementFieldUri;
	}

	@Override
	public String toString()
	{
		final List< String > lines = new ArrayList<>();
		lines.add( "Displacement field transformation: " + getName() );
		lines.add( displacementFieldUri );
		addSources( lines );
		return String.join( "\n", lines );
	}
}

