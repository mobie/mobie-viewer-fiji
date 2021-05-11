package de.embl.cba.mobie2.transform;

public class SourceTransformerSupplier
{
	private AffineSourceTransformer affine;
	private GridSourceTransformer grid;

	public SourceTransformerSupplier( SourceTransformer sourceTransformer ) {
		if ( sourceTransformer instanceof  AffineSourceTransformer ) {
			this.affine = (AffineSourceTransformer) sourceTransformer;
		} else if ( sourceTransformer instanceof  GridSourceTransformer ) {
			this.grid = (GridSourceTransformer) sourceTransformer;
		}
	}

	public SourceTransformer get()
	{
		if ( affine != null ) return affine;
		else if ( grid != null ) return grid;
		else throw new RuntimeException( "No SourceTransformer set" );
	}
}
