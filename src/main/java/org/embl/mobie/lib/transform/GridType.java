package org.embl.mobie.lib.transform;

public enum GridType
{
	None,
	Stitched,
	Transformed;

	public static GridType fromString( String string ) {
		for (GridType gridType : GridType.values()) {
			if (gridType.name().compareToIgnoreCase( string ) == 0) {
				return gridType;
			}
		}
		return null;
	}
}
