package org.embl.mobie.lib.transform;

/**
 * JSON metadata for persisted displacement fields.
 * This class is intentionally plain so it can be directly de/serialized with Gson.
 */
public class DisplacementFieldStorageMetadata
{
	public String type = "inverse_displacement_field";
	public int version = 1;
	public int numDimensions;
	public int componentAxis = 0;
	public int[] size;
	public double[] spacing;
	public double[] origin;
	public String dataType = "float32";
	public String byteOrder = "BIG_ENDIAN";
	public String convention = "x=y+d(y)";
	public String rawPath;
	public String sourceElastixTransformParametersFile;
	public Integer samplingFactor;
	public Double optimizerMaxStep;
	public Double optimizerTolerance;
	public Integer optimizerMaxIterations;
	public String computeTimestamp;
	public String comments;
	public QualityMetrics quality;
	public DisplacementStatistics displacement;

	public static class QualityMetrics
	{
		public int numSamples;
		public double meanRoundTripError;
		public double maxRoundTripError;
	}

	public static class DisplacementStatistics
	{
		public int numSamples;
		public double medianMagnitude;
		public double maxMagnitude;
	}
}

