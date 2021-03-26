package de.embl.cba.mobie2.color;

import com.google.gson.annotations.SerializedName;

public enum BlendingMode
{
	@SerializedName("sum")
	Sum,
	@SerializedName("average")
	Average,
	@SerializedName("occluding")
	Occluding,
}
