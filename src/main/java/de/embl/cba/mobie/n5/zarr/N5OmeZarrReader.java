/**
 * Copyright (c) 2019, Stephan Saalfeld
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;


/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 *
 */
public class N5OmeZarrReader extends N5FSReader {
    private static final String DEFAULT_SEPARATOR = ".";

    protected static Version VERSION = new Version(2, 0, 0);

    protected static final String zarrayFile = ".zarray";
    protected static final String zattrsFile = ".zattrs";
    protected static final String zgroupFile = ".zgroup";

    HashMap<String, Integer> axesMap = new HashMap<>();

    static private GsonBuilder initGsonBuilder(final GsonBuilder gsonBuilder) {

        gsonBuilder.registerTypeAdapter(DType.class, new DType.JsonAdapter());
        gsonBuilder.registerTypeAdapter(ZarrCompressor.class, ZarrCompressor.jsonAdapter);
        gsonBuilder.serializeNulls();

        return gsonBuilder;
    }

    final protected boolean mapN5DatasetAttributes;
    protected String dimensionSeparator;

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path with a custom
     * {@link GsonBuilder} to support custom attributes.
     *
     * @param basePath Zarr base path
     * @param gsonBuilder
     * @param dimensionSeparator
     * @param mapN5DatasetAttributes
     * 			Virtually create N5 dataset attributes (dimensions, blockSize,
     * 			compression, dataType) for datasets such that N5 code that
     * 			reads or modifies these attributes directly works as expected.
     * 			This can lead to name clashes if a zarr container uses these
     * 			attribute keys for other purposes.
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final GsonBuilder gsonBuilder, final String dimensionSeparator, final boolean mapN5DatasetAttributes) throws IOException {

        super(basePath, initGsonBuilder(gsonBuilder));
        this.dimensionSeparator = dimensionSeparator;
        this.mapN5DatasetAttributes = mapN5DatasetAttributes;
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path with a custom
     * {@link GsonBuilder} to support custom attributes.
     *
     * @param basePath Zarr base path
     * @param gsonBuilder
     * @param dimensionSeparator
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final GsonBuilder gsonBuilder, final String dimensionSeparator) throws IOException {

        this(basePath, gsonBuilder, dimensionSeparator, true);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path.
     *
     * @param basePath Zarr base path
     * @param dimensionSeparator
     * @param mapN5DatasetAttributes
     * 			Virtually create N5 dataset attributes (dimensions, blockSize,
     * 			compression, dataType) for datasets such that N5 code that
     * 			reads or modifies these attributes directly works as expected.
     * 			This can lead to name collisions if a zarr container uses these
     * 			attribute keys for other purposes.
     *
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final String dimensionSeparator, final boolean mapN5DatasetAttributes) throws IOException {

        this(basePath, new GsonBuilder(), dimensionSeparator, mapN5DatasetAttributes);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path.
     *
     * @param basePath Zarr base path
     * @param mapN5DatasetAttributes
     * 			Virtually create N5 dataset attributes (dimensions, blockSize,
     * 			compression, dataType) for datasets such that N5 code that
     * 			reads or modifies these attributes directly works as expected.
     * 			This can lead to name collisions if a zarr container uses these
     * 			attribute keys for other purposes.
     *
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final boolean mapN5DatasetAttributes) throws IOException {
        this(basePath, new GsonBuilder(), DEFAULT_SEPARATOR, mapN5DatasetAttributes);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path with a custom
     * {@link GsonBuilder} to support custom attributes.
     *
     * Zarray metadata will be virtually mapped to N5 dataset attributes.
     *
     * @param basePath Zarr base path
     * @param gsonBuilder
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath, final GsonBuilder gsonBuilder) throws IOException {
        this(basePath, gsonBuilder, DEFAULT_SEPARATOR);
    }

    /**
     * Opens an {@link N5OmeZarrReader} at a given base path.
     *
     * Zarray metadata will be virtually mapped to N5 dataset attributes.
     *
     * @param basePath Zarr base path
     * @throws IOException
     */
    public N5OmeZarrReader(final String basePath) throws IOException {

        this(basePath, new GsonBuilder());
    }

    @Override
    public Version getVersion() throws IOException {

        final Path path;
        if (groupExists("/")) {
            path = Paths.get(basePath, zgroupFile);
        } else if (datasetExists("/")) {
            path = Paths.get(basePath, zarrayFile);
        } else {
            return VERSION;
        }

        if (Files.exists(path)) {

            try (final LockedFileChannel lockedFileChannel = LockedFileChannel.openForReading(path)) {
                final HashMap<String, JsonElement> attributes =
                        GsonAttributesParser.readAttributes(
                                Channels.newReader(
                                        lockedFileChannel.getFileChannel(),
                                        StandardCharsets.UTF_8.name()),
                                gson);
                final Integer zarr_format = GsonAttributesParser.parseAttribute(
                        attributes,
                        "zarr_format",
                        Integer.class,
                        gson);

                if (zarr_format != null)
                    return new Version(zarr_format, 0, 0);
            }
        }
        return VERSION;
    }

    /**
     *
     * @return Zarr base path
     */
    @Override
    public String getBasePath() {

        return this.basePath;
    }

    public boolean groupExists(final String pathName) {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zgroupFile);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public ZArrayAttributes getZArraryAttributes(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zarrayFile);
        final HashMap<String, JsonElement> attributes = new HashMap<>();

        if (Files.exists(path)) {

            try (final LockedFileChannel lockedFileChannel = LockedFileChannel.openForReading(path)) {
                attributes.putAll(
                        GsonAttributesParser.readAttributes(
                                Channels.newReader(
                                        lockedFileChannel.getFileChannel(),
                                        StandardCharsets.UTF_8.name()),
                                gson));
            }
        } else System.out.println(path + " does not exist.");

        System.out.println(attributes.keySet());
        JsonElement dimSep = attributes.get("dimension_separator");
        this.dimensionSeparator = dimSep == null ? DEFAULT_SEPARATOR : dimSep.getAsString();

        return new ZArrayAttributes(
                attributes.get("zarr_format").getAsInt(),
                gson.fromJson(attributes.get("shape"), long[].class),
                gson.fromJson(attributes.get("chunks"), int[].class),
                gson.fromJson(attributes.get("dtype"), DType.class),
                gson.fromJson(attributes.get("compressor"), ZarrCompressor.class),
                attributes.get("fill_value").getAsString(),
                attributes.get("order").getAsCharacter(),
                gson.fromJson(attributes.get("filters"), TypeToken.getParameterized(Collection.class, Filter.class).getType()));
    }

    @Override
    public DatasetAttributes getDatasetAttributes(final String pathName) throws IOException {

        final ZArrayAttributes zArrayAttributes = getZArraryAttributes(pathName);
        return zArrayAttributes == null ? null : zArrayAttributes.getDatasetAttributes();
    }

    @Override
    public boolean datasetExists(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zarrayFile);
        return Files.exists(path) && Files.isRegularFile(path) && getDatasetAttributes(pathName) != null;
    }


    /**
     * @returns false if the group or dataset does not exist but also if the
     * 		attempt to access
     */
    @Override
    public boolean exists(final String pathName) {

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
    public HashMap<String, JsonElement> getAttributes(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zattrsFile);
        final HashMap<String, JsonElement> attributes = new HashMap<>();

        if (Files.exists(path)) {
            try (final LockedFileChannel lockedFileChannel = LockedFileChannel.openForReading(path)) {
                attributes.putAll(
                        GsonAttributesParser.readAttributes(
                                Channels.newReader(
                                        lockedFileChannel.getFileChannel(),
                                        StandardCharsets.UTF_8.name()),
                                gson));
            }
        }

        getDimensions(attributes);

        if (mapN5DatasetAttributes && datasetExists(pathName)) {

            final DatasetAttributes datasetAttributes = getZArraryAttributes(pathName).getDatasetAttributes();
            attributes.put("dimensions", gson.toJsonTree(datasetAttributes.getDimensions()));
            attributes.put("blockSize", gson.toJsonTree(datasetAttributes.getBlockSize()));
            attributes.put("dataType", gson.toJsonTree(datasetAttributes.getDataType()));
            attributes.put("compression", gson.toJsonTree(datasetAttributes.getCompression()));
        }

        return attributes;
    }

    private void getDimensions(HashMap<String, JsonElement> attributes) {
        JsonElement multiscales = attributes.get("multiscales");
        if (multiscales != null) {
            JsonElement axes = multiscales.getAsJsonArray().get(0).getAsJsonObject().get("axes");
            setAxes(axes);
        }
    }

    private boolean axesValid(JsonElement axesJson) {
        return ZarrAxes.decode(axesJson.toString()) != null;
    }

    public void setAxes(JsonElement axesJson) {
        if (axesJson != null && axesValid(axesJson)) {
            for (int i = 0; i < axesJson.getAsJsonArray().size(); i++) {
                String elem = axesJson.getAsJsonArray().get(i).getAsString();
                this.axesMap.put(elem, i);
            }
        }
    }

    public HashMap<String, Integer> getAxes() {
        return this.axesMap;
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
            final long... gridPosition) throws IOException {

        final int[] blockSize = datasetAttributes.getBlockSize();
        final DType dType = datasetAttributes.getDType();

        final ByteArrayDataBlock byteBlock = dType.createByteBlock(blockSize, gridPosition);

        final BlockReader reader = datasetAttributes.getCompression().getReader();
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
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

        /* TODO I do not think that makes sense, F order should be opened transposed, the consumer can decide what to do with them? */
//		if (!datasetAttributes.isRowMajor()) {
//
//			final long[] blockDimensions = new long[blockSize.length];
//			Arrays.setAll(blockDimensions, d -> blockSize[d]);
//
//			switch (datasetAttributes.getDataType()) {
//			case INT8:
//			case UINT8: {
//					final byte[] dataBlockData = (byte[])dataBlock.getData();
//					final ArrayImg<ByteType, ByteArray> src = ArrayImgs.bytes(dataBlockData.clone(), blockDimensions);
//					final ArrayImg<ByteType, ByteArray> dst = ArrayImgs.bytes(dataBlockData.clone(), blockDimensions);
//					copyTransposed(src, dst);
//				}
//				break;
//			case INT16:
//			case UINT16: {
//					final short[] dataBlockData = (short[])dataBlock.getData();
//					final ArrayImg<ShortType, ShortArray> src = ArrayImgs.shorts(dataBlockData.clone(), blockDimensions);
//					final ArrayImg<ShortType, ShortArray> dst = ArrayImgs.shorts(dataBlockData.clone(), blockDimensions);
//					copyTransposed(src, dst);
//				}
//				break;
//			case INT32:
//			case UINT32: {
//					final int[] dataBlockData = (int[])dataBlock.getData();
//					final ArrayImg<IntType, IntArray> src = ArrayImgs.ints(dataBlockData.clone(), blockDimensions);
//					final ArrayImg<IntType, IntArray> dst = ArrayImgs.ints(dataBlockData.clone(), blockDimensions);
//					copyTransposed(src, dst);
//				}
//				break;
//			case INT64:
//			case UINT64: {
//					final long[] dataBlockData = (long[])dataBlock.getData();
//					final ArrayImg<LongType, LongArray> src = ArrayImgs.longs(dataBlockData.clone(), blockDimensions);
//					final ArrayImg<LongType, LongArray> dst = ArrayImgs.longs(dataBlockData.clone(), blockDimensions);
//					copyTransposed(src, dst);
//				}
//				break;
//			case FLOAT32: {
//					final float[] dataBlockData = (float[])dataBlock.getData();
//					final ArrayImg<FloatType, FloatArray> src = ArrayImgs.floats(dataBlockData.clone(), blockDimensions);
//					final ArrayImg<FloatType, FloatArray> dst = ArrayImgs.floats(dataBlockData.clone(), blockDimensions);
//					copyTransposed(src, dst);
//				}
//				break;
//			case FLOAT64: {
//					final double[] dataBlockData = (double[])dataBlock.getData();
//					final ArrayImg<DoubleType, DoubleArray> src = ArrayImgs.doubles(dataBlockData.clone(), blockDimensions);
//					final ArrayImg<DoubleType, DoubleArray> dst = ArrayImgs.doubles(dataBlockData.clone(), blockDimensions);
//					copyTransposed(src, dst);
//				}
//				break;
//			}
//		}

        return dataBlock;
    }

    protected static <T extends Type<T>> void copyTransposed(
            final RandomAccessibleInterval<? extends T> src,
            final RandomAccessibleInterval<? extends T> dst) {

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
            final long... gridPosition) throws IOException {

        final ZarrDatasetAttributes zarrDatasetAttributes;
        if (datasetAttributes instanceof ZarrDatasetAttributes)
            zarrDatasetAttributes = (ZarrDatasetAttributes) datasetAttributes;
        else
            zarrDatasetAttributes = getZArraryAttributes(pathName).getDatasetAttributes();

        Path path = Paths.get(
                basePath,
                removeLeadingSlash(pathName),
                getZarrDataBlockPath(
                        gridPosition,
                        dimensionSeparator,
                        zarrDatasetAttributes.isRowMajor()).toString());
        System.out.println("readBlock path" + path);
        if (!Files.exists(path)) {
            return null;
        }

        try (final LockedFileChannel lockedChannel = LockedFileChannel.openForReading(path)) {
            return readBlock(Channels.newInputStream(lockedChannel.getFileChannel()), zarrDatasetAttributes, gridPosition);
        }
    }

    @Override
    public String[] list(final String pathName) throws IOException {

        final Path path = Paths.get(basePath, removeLeadingSlash(pathName));
        try (final Stream<Path> pathStream = Files.list(path)) {

            return pathStream
                    .filter(a -> Files.isDirectory(a))
                    .map(a -> path.relativize(a).toString())
                    .filter(a -> exists(pathName + "/" + a))
                    .toArray(n -> new String[n]);
        }
    }

    /**
     * Constructs the path for a data block in a dataset at a given grid position.
     *
     * The returned path is
     * <pre>
     * $datasetPathName/$gridPosition[n]$dimensionSeparator$gridPosition[n-1]$dimensionSeparator[...]$dimensionSeparator$gridPosition[0]
     * </pre>
     *
     * This is the file into which the data block will be stored.
     *
     * @param datasetPathName
     * @param gridPosition
     * @param dimensionSeparator
     *
     * @return
     */
    protected static Path getZarrDataBlockPath(
            final long[] gridPosition,
            final String dimensionSeparator,
            final boolean isRowMajor) {

        final StringBuilder pathStringBuilder = new StringBuilder();
        if (isRowMajor) {
            pathStringBuilder.append(gridPosition[gridPosition.length - 1]);
            for (int i = gridPosition.length - 2; i >= 0; --i) {
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
        System.out.println("Path" + Paths.get(pathStringBuilder.toString()));
        return Paths.get(pathStringBuilder.toString());
    }
}
