package de.embl.cba.mobie2.transform;

import de.embl.cba.mobie2.transform.AffineSourceTransformer;
import de.embl.cba.mobie2.transform.AutoGridSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;

import java.io.Serializable;

public class SourceTransformerSupplier
{
	private AffineSourceTransformer affine;
	private AutoGridSourceTransformer autoGrid;

	public SourceTransformer get()
	{
		if ( affine != null ) return affine;
		else if ( autoGrid != null ) return autoGrid;
		else throw new RuntimeException( "No SourceTransformer set" );
	}
}
