package de.embl.cba.mobie.bdv.render;

import com.google.gson.annotations.SerializedName;

/*
 * Constants to define the blending mode of sources.
 * These constants are used within {@link AccumulateMixedProjectorARGB}.
 *
 * Sum:
 * The ARGB values of the source will be added to the final ARGB to be displayed.
 *
 * Average:
 * The ARGB values of all sources with the Average projection mode will first be averaged
 * before being added to the final ARGB to be displayed.
 * This is useful for overlapping electron microscopy data sets.
 *
 * Occluding:
 * For a given pixel, if there are sources with the Exclusive projection mode and with
 * an alpha value larger than zero, only these source will be displayed.
 * The pixels of all other sources will not be visible.
 * This is useful, e.g., if there is a region where one source contains information
 * at a higher resolution than another source. Selecting the Exclusive projection mode
 * can be used to only show this source.
 *
 */
public enum BlendingMode
{
	@SerializedName("sum")
	Sum,
	@SerializedName("sumOccluding")
	SumOccluding,
	@SerializedName("average") @Deprecated
	Average,
	@SerializedName("averageOccluding") @Deprecated
	AverageOccluding;

	// To use in Commands until they can do enums as choices
	public static final String SUM = "Sum";
	public static final String SUM_OCCLUDING = "SumOccluding";

	// To use as a key for the xml
	// underscore necessary for valid xml element to store in @see DisplaySettings
	public static final String BLENDING_MODE = "Blending Mode";

	public static boolean isOccluding( BlendingMode blendingMode )
	{
		return ( blendingMode.equals( BlendingMode.AverageOccluding ) || blendingMode.equals( BlendingMode.SumOccluding ) );
	}
}
