package de.embl.cba.mobie.transform;

public class BdvLocationSupplier
{
	private double[] normalizedAffine;
	private double[] position;

	public BdvLocationSupplier( AffineViewerTransform affineViewerTransform ) {
		if ( affineViewerTransform.getType() == BdvLocationType.NormalisedViewerTransform ) {
		} else if ( affineViewerTransform.getType() == BdvLocationType.Position3d ) {
			this.position = affineViewerTransform.getParameters();
		}
	}

	public AffineViewerTransform get()
	{
		if ( normalizedAffine != null )
		{
			final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( BdvLocationType.NormalisedViewerTransform, normalizedAffine );
			return affineViewerTransform;
		}
		else if ( position != null )
		{
			final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( BdvLocationType.Position3d, position );
			return affineViewerTransform;
		}
		else throw new UnsupportedOperationException( "No viewer transform found." );
	}
}
