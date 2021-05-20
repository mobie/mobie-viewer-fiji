package de.embl.cba.mobie2.transform;

public class BdvLocationSupplier
{
	private double[] normalizedAffine;
	private double[] position;

	public BdvLocationSupplier( BdvLocation bdvLocation ) {
		if ( bdvLocation.type == BdvLocationType.NormalisedViewerTransform ) {
			this.normalizedAffine = bdvLocation.doubles;
		} else if ( bdvLocation.type == BdvLocationType.Position3d ) {
			this.position = bdvLocation.doubles;
		}
	}

	public BdvLocation get()
	{
		if ( normalizedAffine != null )
		{
			final BdvLocation bdvLocation = new BdvLocation( BdvLocationType.NormalisedViewerTransform, normalizedAffine );
			return bdvLocation;
		}
		else if ( position != null )
		{
			final BdvLocation bdvLocation = new BdvLocation( BdvLocationType.Position3d, position );
			return bdvLocation;
		}
		else throw new UnsupportedOperationException( "No viewer transform found." );
	}
}
