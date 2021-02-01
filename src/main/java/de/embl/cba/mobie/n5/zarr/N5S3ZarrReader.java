/**
 * Copyright (c) 2019, Stephan Saalfeld
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.embl.cba.mobie.n5.zarr;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;


/**
 * Attempt at a diamond inheritance solution for S3+Zarr.
 *
 */
public class N5S3ZarrReader extends N5AmazonS3Reader
{
	private static final String zarrayFile = N5ZarrReader.zarrayFile;
	private static final String zattrsFile = N5ZarrReader.zattrsFile;
	private static final String zgroupFile = N5ZarrReader.zgroupFile;

	final protected boolean mapN5DatasetAttributes;
	final protected String dimensionSeparator;
	private final String serviceEndpoint;

	public N5S3ZarrReader( AmazonS3 s3, String serviceEndpoint, String bucketName, String containerPath ) throws IOException
	{
		super(s3, bucketName, containerPath, initGsonBuilder(new GsonBuilder()));
		this.serviceEndpoint = serviceEndpoint; // for debugging
		dimensionSeparator = ".";
		mapN5DatasetAttributes = true;
	}

	public AmazonS3 getS3()
	{
		return s3;
	}

	public String getBucketName()
	{
		return bucketName;
	}

	public String getContainerPath()
	{
		return containerPath;
	}

	public String getServiceEndpoint()
	{
		return serviceEndpoint;
	}

	//
	// Local helpers. May should live elsewhere
	//

	/**
	 * Helper to encapsulate building the object key for a file like
	 * .zarray or .zgroup within any given path.
	 *
	 * @param pathName
	 * @param file One of .zarray, .zgroup or .zattrs
	 * @return
	 */
	private String objectFile(final String pathName, String file)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(containerPath);
		String cleaned = removeLeadingSlash(pathName);
		if (!cleaned.isEmpty()) {
			sb.append('/');
			sb.append(cleaned);
		}
		sb.append('/');
		sb.append(file);
		return sb.toString();
	}

	//
	// Methods from N5ZarrReader which could be extracted
	//
	static private GsonBuilder initGsonBuilder(final GsonBuilder gsonBuilder)
	{
		gsonBuilder.registerTypeAdapter(DType.class, new DType.JsonAdapter());
		gsonBuilder.registerTypeAdapter(ZarrCompressor.class, ZarrCompressor.jsonAdapter);
		gsonBuilder.serializeNulls();

		return gsonBuilder;
	}

	@Override
	public Version getVersion() throws IOException
	{
		HashMap< String, JsonElement> meta;
		meta = readJson(objectFile("", zgroupFile));
		if (meta == null) {
			meta = readJson(objectFile("", zarrayFile));
		}

		if (meta != null) {

			final Integer zarr_format = GsonAttributesParser.parseAttribute(
					meta,
					"zarr_format",
					Integer.class,
					gson);

			if (zarr_format != null)
				return new Version(zarr_format, 0, 0);
		}

		return VERSION;
	}

	// remove getBasePath

	public boolean groupExists(final String pathName) {
		return exists(objectFile(pathName, zgroupFile));
	}

	public ZArrayAttributes getZArraryAttributes(final String pathName) throws IOException
	{
		final String path = objectFile(pathName, zarrayFile);
		HashMap< String, JsonElement> attributes = readJson(path);

		if (attributes == null) {
			System.out.println(path.toString() + " does not exist.");
			attributes = new HashMap<>();
		}

		return new ZArrayAttributes(
				attributes.get( "zarr_format" ).getAsInt(),
				gson.fromJson(attributes.get("shape"), long[].class),
				gson.fromJson(attributes.get("chunks"), int[].class),
				gson.fromJson(attributes.get("dtype"), DType.class),
				gson.fromJson(attributes.get("compressor"), ZarrCompressor.class),
				attributes.get("fill_value").getAsString(),
				attributes.get("order").getAsCharacter(),
				gson.fromJson(attributes.get("filters"), TypeToken.getParameterized( Collection.class, Filter.class).getType()));
	}

	@Override
	public DatasetAttributes getDatasetAttributes(final String pathName) throws IOException
	{
		final ZArrayAttributes zArrayAttributes = getZArraryAttributes(pathName);
		return zArrayAttributes == null ? null : zArrayAttributes.getDatasetAttributes();
	}

	@Override
	public boolean datasetExists(final String pathName) throws IOException
	{
		final String path = objectFile(pathName, zarrayFile);
		return readJson(path) != null;
	}

	/**
	 * CHANGE: rename to not overwrite the AWS list objects version
	 * @returns false if the group or dataset does not exist but also if the
	 * 		attempt to access
	 */
	// @Override
	public boolean zarrExists(final String pathName)
	{
		try {
			return groupExists(pathName) || datasetExists(pathName);
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * If {@link #mapN5DatasetAttributes} is set, dataset attributes will
	 * override attributes with the same key.
	 */
	@Override
	public HashMap< String, JsonElement> getAttributes(final String pathName) throws IOException
	{
		final String path = objectFile(pathName, zattrsFile);
		HashMap< String, JsonElement> attributes = readJson(path);

		if (attributes == null) {
			attributes = new HashMap<>();
		}

		if (mapN5DatasetAttributes && datasetExists(pathName)) {

			final DatasetAttributes datasetAttributes = getZArraryAttributes(pathName).getDatasetAttributes();
			attributes.put("dimensions", gson.toJsonTree(datasetAttributes.getDimensions()));
			attributes.put("blockSize", gson.toJsonTree(datasetAttributes.getBlockSize()));
			attributes.put("dataType", gson.toJsonTree(datasetAttributes.getDataType()));
			attributes.put("compression", gson.toJsonTree(datasetAttributes.getCompression()));
		}

		return attributes;
	}

	/**
	 * Reads a {@link DataBlock} from an {@link InputStream}.
	 *
	 * @param in
	 * @param datasetAttributes
	 * @param gridPosition
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("incomplete-switch")
	public static DataBlock<?> readBlock(
			final InputStream in,
			final ZarrDatasetAttributes datasetAttributes,
			final long... gridPosition) throws IOException
	{
		final int[] blockSize = datasetAttributes.getBlockSize();
		final DType dType = datasetAttributes.getDType();

		final ByteArrayDataBlock byteBlock = dType.createByteBlock(blockSize, gridPosition);

		final BlockReader reader = datasetAttributes.getCompression().getReader();
		reader.read(byteBlock, in);

		switch (dType.getDataType()) {
			case UINT8:
			case INT8:
				return byteBlock;
		}

		/* else translate into target type */
		final DataBlock<?> dataBlock = dType.createDataBlock(blockSize, gridPosition);
		final ByteBuffer byteBuffer = byteBlock.toByteBuffer();
		byteBuffer.order(dType.getOrder());
		dataBlock.readData(byteBuffer);

		return dataBlock;
	}

	protected static <T extends Type<T>> void copyTransposed(
			final RandomAccessibleInterval<? extends T> src,
			final RandomAccessibleInterval<? extends T> dst)
	{
		/* transpose */
		final int n = src.numDimensions();
		final int[] lut = new int[n];
		Arrays.setAll(lut, d -> n - 1 - d);
		final IntervalView<? extends T> dstTransposed = Views.permuteCoordinates(dst, lut);

		/* copy */
		final Cursor<? extends T> cSrc = Views.flatIterable(src).cursor();
		final Cursor<? extends T> cDst = Views.flatIterable(dstTransposed).cursor();
		while (cDst.hasNext())
			cDst.next().set(cSrc.next());
	}

	@Override
	public DataBlock<?> readBlock(
			final String pathName,
			final DatasetAttributes datasetAttributes,
			final long... gridPosition) throws IOException
	{
		final ZarrDatasetAttributes zarrDatasetAttributes;
		if (datasetAttributes instanceof ZarrDatasetAttributes)
			zarrDatasetAttributes = (ZarrDatasetAttributes)datasetAttributes;
		else
			zarrDatasetAttributes = getZArraryAttributes(pathName).getDatasetAttributes();

		final String dataBlockKey =
				objectFile(pathName,
					getZarrDataBlockPath(
						gridPosition,
						dimensionSeparator,
						zarrDatasetAttributes.isRowMajor()).toString());

		// Currently exists() appends "/"
		//		if (!exists(dataBlockKey))
		//			return null;

		try {
			try( final InputStream in = this.readS3Object(dataBlockKey)) {
				return readBlock(in, zarrDatasetAttributes, gridPosition);
			}
		} catch (AmazonS3Exception ase) {
			if ("NoSuchKey".equals(ase.getErrorCode())) {
				return null;
			}
			throw ase;
		}
	}

	// CHANGE: remove N5FSReader.list(String) implementation in favor of AWS

	/**
	 * CHANGE: return String rather than Path, fixed javadoc
	 * Constructs the path for a data block in a dataset at a given grid position.
	 *
	 * The returned path is
	 * <pre>
	 * $datasetPathName/$gridPosition[n]$dimensionSeparator$gridPosition[n-1]$dimensionSeparator[...]$dimensionSeparator$gridPosition[0]
	 * </pre>
	 *
	 * This is the file into which the data block will be stored.
	 *
	 * @param gridPosition
	 * @param dimensionSeparator
	 *
	 * @return
	 */
	protected static String getZarrDataBlockPath(
			final long[] gridPosition,
			final String dimensionSeparator,
			final boolean isRowMajor)
	{
		final StringBuilder pathStringBuilder = new StringBuilder();
		if (isRowMajor) {
			pathStringBuilder.append(gridPosition[gridPosition.length - 1]);
			for (int i = gridPosition.length - 2; i >= 0 ; --i) {
				pathStringBuilder.append(dimensionSeparator);
				pathStringBuilder.append(gridPosition[i]);
			}
		} else {
			pathStringBuilder.append(gridPosition[0]);
			for (int i = 1; i < gridPosition.length; ++i) {
				pathStringBuilder.append(dimensionSeparator);
				pathStringBuilder.append(gridPosition[i]);
			}
		}

		return pathStringBuilder.toString();
	}

	//
	// Helpers from N5AmazonS3Reader which could be extracted
	//

	/**
	 * Copied from getAttributes but doesn't change the objectPath in anyway.
	 * CHANGES: returns null rather than empty hash map
	 *
	 * @param objectPath
	 * @return null if the object does not exist, otherwise the loaded attributes.
	 * @throws IOException
	 */
	public HashMap< String, JsonElement> readJson(String objectPath) throws IOException
	{
		if (! this.s3.doesObjectExist(this.bucketName, objectPath)) {
			return null;
//			throw new UnsupportedOperationException( this.bucketName + " " + objectPath + " does not exist." );
		} else {
			InputStream in = this.readS3Object(objectPath);
			Throwable var4 = null;

			HashMap var5;
			try {
				var5 = GsonAttributesParser.readAttributes(new InputStreamReader(in), this.gson);
			} catch ( Throwable var14) {
				var4 = var14;
				throw var14;
			} finally {
				if (in != null) {
					if (var4 != null) {
						try {
							in.close();
						} catch ( Throwable var13) {
							var4.addSuppressed(var13);
						}
					} else {
						in.close();
					}
				}

			}

			return var5;
		}
	}

}