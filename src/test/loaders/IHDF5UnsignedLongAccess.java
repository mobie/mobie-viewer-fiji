package de.embl.cba.platynereis.labels.loaders;

import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.ViewLevelId;

public interface IHDF5UnsignedLongAccess
{
	public DimsAndExistence getDimsAndExistence( final ViewLevelId id );

	public long[] readLongMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;

	public long[] readLongMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final long[] dataBlock ) throws InterruptedException;

	public float[] readLongMDArrayBlockWithOffsetAsFloat( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;

	public float[] readLongMDArrayBlockWithOffsetAsFloat( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final float[] dataBlock ) throws InterruptedException;

	public void closeAllDataSets();

	public void close();
}
