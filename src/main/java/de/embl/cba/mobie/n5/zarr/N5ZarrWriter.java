/**
 * Copyright (c) 2017, Stephan Saalfeld
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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Stephan Saalfeld
 */
public class N5ZarrWriter extends N5ZarrReader implements N5Writer
{

	/**
	 * Opens an {@link N5ZarrWriter} at a given base path with a custom
	 * {@link GsonBuilder} to support custom attributes.
	 *
	 * If the base path does not exist, it will be created.
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
	 *    if the base path cannot be written to or cannot be created,
	 */
	public N5ZarrWriter( final String basePath, final GsonBuilder gsonBuilder, final String dimensionSeparator, final boolean mapN5DatasetAttributes) throws IOException
	{

		super(basePath, gsonBuilder, dimensionSeparator, mapN5DatasetAttributes);
		createDirectories( Paths.get(basePath));
	}

	/**
	 * Opens an {@link N5ZarrWriter} at a given base path with a custom
	 * {@link GsonBuilder} to support custom attributes.
	 *
	 * If the base path does not exist, it will be created.
	 *
	 * @param basePath Zarr base path
	 * @param gsonBuilder
	 * @param dimensionSeparator
	 * @throws IOException
	 */
	public N5ZarrWriter( final String basePath, final GsonBuilder gsonBuilder, final String dimensionSeparator) throws IOException
	{

		this(basePath, gsonBuilder, dimensionSeparator, true);
	}

	/**
	 * Opens an {@link N5ZarrWriter} at a given base path.
	 *
	 * If the base path does not exist, it will be created.
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
	public N5ZarrWriter( final String basePath, final String dimensionSeparator, final boolean mapN5DatasetAttributes) throws IOException
	{

		this(basePath, new GsonBuilder(), dimensionSeparator, mapN5DatasetAttributes);
	}

	/**
	 * Opens an {@link N5ZarrWriter} at a given base path.
	 *
	 * If the base path does not exist, it will be created.
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
	public N5ZarrWriter( final String basePath, final boolean mapN5DatasetAttributes) throws IOException
	{

		this(basePath, new GsonBuilder(), ".", mapN5DatasetAttributes);
	}

	/**
	 * Opens an {@link N5ZarrWriter} at a given base path with a custom
	 * {@link GsonBuilder} to support custom attributes.
	 *
	 * If the base path does not exist, it will be created.
	 *
	 * @param basePath Zarr base path
	 * @param gsonBuilder
	 * @throws IOException
	 *    if the base path cannot be written to or cannot be created,
	 */
	public N5ZarrWriter( final String basePath, final GsonBuilder gsonBuilder) throws IOException
	{

		this(basePath, gsonBuilder, ".");
	}

	/**
	 * Opens an {@link N5ZarrWriter} at a given base path.
	 *
	 * If the base path does not exist, it will be created.
	 *
	 * If the base path exists and if the N5 version of the container is
	 * compatible with this implementation, the N5 version of this container
	 * will be set to the current N5 version of this implementation.
	 *
	 * @param basePath n5 base path
	 * @param gsonBuilder
	 * @throws IOException
	 *    if the base path cannot be written to or cannot be created,
	 */
	public N5ZarrWriter(final String basePath) throws IOException
	{

		this(basePath, new GsonBuilder());
	}

	@Override
	public void createGroup(final String pathName) throws IOException
	{

		final Path path = Paths.get(basePath, pathName);
		createDirectories(path);

		final Path root = Paths.get(basePath);
		Path parent = path;
		for (setGroupVersion(parent); !parent.equals(root);) {
			parent = parent.getParent();
			setGroupVersion(parent);
		}
	}

	protected void setGroupVersion(final Path groupPath) throws IOException
	{

		final Path path = groupPath.resolve(zgroupFile);
		final HashMap< String, JsonElement> map = new HashMap<>();
		map.put("zarr_format", new JsonPrimitive(N5ZarrReader.VERSION.getMajor()));

		try (final N5FSReader.LockedFileChannel lockedFileChannel = N5FSReader.LockedFileChannel.openForWriting(path)) {
			lockedFileChannel.getFileChannel().truncate(0);
			GsonAttributesParser.writeAttributes( Channels.newWriter(lockedFileChannel.getFileChannel(), StandardCharsets.UTF_8.name()), map, gson);
		}
	}

	public void setZArrayAttributes(
			final String pathName,
			final ZArrayAttributes attributes) throws IOException
	{

		final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zarrayFile);
		final HashMap< String, JsonElement> map = new HashMap<>();
		GsonAttributesParser.insertAttributes(map, attributes.asMap(), gson);

		try (final N5FSReader.LockedFileChannel lockedFileChannel = N5FSReader.LockedFileChannel.openForWriting(path)) {

			lockedFileChannel.getFileChannel().truncate(0);
			GsonAttributesParser.writeAttributes( Channels.newWriter(lockedFileChannel.getFileChannel(), StandardCharsets.UTF_8.name()), map, gson);
		}
	}

	@Override
	public void setDatasetAttributes(
			final String pathName,
			final DatasetAttributes datasetAttributes) throws IOException
	{

		final long[] shape = datasetAttributes.getDimensions().clone();
		Utils.reorder(shape);
		final int[] chunks = datasetAttributes.getBlockSize().clone();
		Utils.reorder(chunks);

		final ZArrayAttributes zArrayAttributes = new ZArrayAttributes(
				N5ZarrReader.VERSION.getMajor(),
				shape,
				chunks,
				new DType(datasetAttributes.getDataType()),
				ZarrCompressor.fromCompression(datasetAttributes.getCompression()),
				"0",
				'C',
				null);

		setZArrayAttributes(pathName, zArrayAttributes);
	}

	@Override
	public void createDataset(
			final String pathName,
			final DatasetAttributes datasetAttributes) throws IOException
	{

		/* create parent groups */
		final String parentGroup = pathName.substring(0, removeTrailingSlash(pathName).lastIndexOf('/'));
		if (!parentGroup.equals(""))
			createGroup(parentGroup);

		final Path path = Paths.get(basePath, pathName);
		createDirectories(path);

		setDatasetAttributes(pathName, datasetAttributes);
	}

	@Override
	public void setAttributes(
			final String pathName,
			Map< String, ?> attributes) throws IOException
	{

		final Path path = Paths.get(basePath, removeLeadingSlash(pathName), zattrsFile);
		final HashMap< String, JsonElement> map = new HashMap<>();

		try (final N5FSReader.LockedFileChannel lockedFileChannel = N5FSReader.LockedFileChannel.openForWriting(path)) {
			map.putAll(
					GsonAttributesParser.readAttributes(
							Channels.newReader(
									lockedFileChannel.getFileChannel(),
									StandardCharsets.UTF_8.name()),
							gson));

			if (mapN5DatasetAttributes && datasetExists(pathName)) {

				attributes = new HashMap<>(attributes);
				ZArrayAttributes zArrayAttributes = getZArraryAttributes(pathName);
				long[] shape;
				int[] chunks;
				final DType dtype;
				final ZarrCompressor compressor;
				final boolean isRowMajor = zArrayAttributes.getOrder() == 'C';

				if (attributes.containsKey("dimensions")) {
					shape = (long[])attributes.get("dimensions");
					attributes.remove("dimensions");
					if (isRowMajor) {
						shape = shape.clone();
						Utils.reorder(shape);
					}
				} else
					 shape = zArrayAttributes.getShape();

				if (attributes.containsKey("blockSize")) {
					chunks = (int[])attributes.get("blockSize");
					attributes.remove("blockSize");
					if (isRowMajor) {
						chunks = chunks.clone();
						Utils.reorder(chunks);
					}
				} else
					chunks = zArrayAttributes.getChunks();

				if (attributes.containsKey("dataType")) {
					dtype = new DType(( DataType )attributes.get("dataType"));
					attributes.remove("dataType");
				} else
					dtype = zArrayAttributes.getDType();

				if (attributes.containsKey("compression")) {
					compressor = ZarrCompressor.fromCompression(( Compression )attributes.get("compression"));
					attributes.remove("compression");
					/* fails with null when compression is not supported by Zarr
					 * TODO invent meaningful error behavior */
				} else
					compressor = zArrayAttributes.getCompressor();

				zArrayAttributes = new ZArrayAttributes(
						zArrayAttributes.getZarrFormat(),
						shape,
						chunks,
						dtype,
						compressor,
						zArrayAttributes.getFillValue(),
						zArrayAttributes.getOrder(),
						zArrayAttributes.getFilters());

				setZArrayAttributes(pathName, zArrayAttributes);
			}

			GsonAttributesParser.insertAttributes(map, attributes, gson);

			lockedFileChannel.getFileChannel().truncate(0);
			GsonAttributesParser.writeAttributes(
					Channels.newWriter(lockedFileChannel.getFileChannel(), StandardCharsets.UTF_8.name()),
					map,
					gson);
		}
	}

	public static byte[] padCrop(
			final byte[] src,
			final int[] srcBlockSize,
			final int[] dstBlockSize,
			final int nBytes,
			final int nBits,
			final byte[] fill_value) {

		assert srcBlockSize.length == dstBlockSize.length : "Dimensions do not match.";

		final int n = srcBlockSize.length;

		if (nBytes != 0) {
			final int[] srcStrides = new int[n];
			final int[] dstStrides = new int[n];
			final int[] srcSkip = new int[n];
			final int[] dstSkip  = new int[n];
			srcStrides[0] = dstStrides[0] = nBytes;
			for (int d = 1; d < n; ++d) {
				srcStrides[d] = srcBlockSize[d] * srcBlockSize[d - 1];
				dstStrides[d] = dstBlockSize[d] * dstBlockSize[d - 1];
			}
			for (int d = 0; d < n; ++d) {
				srcSkip[d] = Math.max(1, dstBlockSize[d] - srcBlockSize[d]);
				dstSkip[d] = Math.max(1, srcBlockSize[d] - dstBlockSize[d]);
			}

			/* this is getting hairy, ImgLib2 alternative */
			/* byte images with 0-dimension d[0] * nBytes */
			final long[] srcIntervalDimensions = new long[n];
			final long[] dstIntervalDimensions = new long[n];
			srcIntervalDimensions[0] = srcBlockSize[0] * nBytes;
			dstIntervalDimensions[0] = dstBlockSize[0] * nBytes;
			for (int d = 1; d < n; ++d) {
				srcIntervalDimensions[d] = srcBlockSize[d];
				dstIntervalDimensions[d] = dstBlockSize[d];
			}

			final byte[] dst = new byte[(int)Intervals.numElements(dstIntervalDimensions)];
			/* fill dst */
			for (int i = 0, j = 0; i < n; ++i) {
				dst[i] = fill_value[j];
				if (++j == fill_value.length)
					j = 0;
			}
			final ArrayImg<ByteType, ByteArray> srcImg = ArrayImgs.bytes(src, srcIntervalDimensions);
			final ArrayImg<ByteType, ByteArray> dstImg = ArrayImgs.bytes(dst, dstIntervalDimensions);

			final FinalInterval intersection = Intervals.intersect(srcImg, dstImg);
			final Cursor<ByteType> srcCursor = Views.interval(srcImg, intersection).cursor();
			final Cursor<ByteType> dstCursor = Views.interval(dstImg, intersection).cursor();
			while (srcCursor.hasNext())
				dstCursor.next().set(srcCursor.next());

			return dst;
		} else {
			/* TODO deal with bit streams */
			return null;
		}
	}

	/**
	 * Writes a {@link DataBlock} into an {@link OutputStream}.
	 *
	 * @param out
	 * @param datasetAttributes
	 * @param dataBlock
	 * @throws IOException
	 */
	public static <T> void writeBlock(
			final OutputStream out,
			final ZarrDatasetAttributes datasetAttributes,
			final DataBlock<T> dataBlock) throws IOException
	{

		final int[] blockSize = datasetAttributes.getBlockSize();
		final DType dType = datasetAttributes.getDType();
		final DataOutputStream dos = new DataOutputStream(out);
		final BlockWriter writer = datasetAttributes.getCompression().getWriter();

		if (!Arrays.equals(blockSize, dataBlock.getSize())) {

			final byte[] padCropped = padCrop(
					dataBlock.toByteBuffer().array(),
					dataBlock.getSize(),
					blockSize,
					dType.getNBytes(),
					dType.getNBits(),
					datasetAttributes.getFillBytes());

			final DataBlock<byte[]> padCroppedDataBlock =
					new ByteArrayDataBlock(
							blockSize,
							dataBlock.getGridPosition(),
							padCropped);

			writer.write(padCroppedDataBlock, out);

		} else {

			writer.write(dataBlock, out);
		}
	}

	@Override
	public <T> void writeBlock(
			final String pathName,
			final DatasetAttributes datasetAttributes,
			final DataBlock<T> dataBlock) throws IOException
	{

		final ZarrDatasetAttributes zarrDatasetAttributes;
		if (datasetAttributes instanceof ZarrDatasetAttributes)
			zarrDatasetAttributes = (ZarrDatasetAttributes)datasetAttributes;
		else
			zarrDatasetAttributes = getZArraryAttributes(pathName).getDatasetAttributes();

		final Path path = Paths.get(
				basePath,
				removeLeadingSlash(pathName),
				getZarrDataBlockPath(
						dataBlock.getGridPosition(),
						dimensionSeparator,
						zarrDatasetAttributes.isRowMajor()).toString());
		createDirectories(path.getParent());
		try (final N5FSReader.LockedFileChannel lockedChannel = N5FSReader.LockedFileChannel.openForWriting(path)) {

			lockedChannel.getFileChannel().truncate(0);
			writeBlock(
					Channels.newOutputStream(lockedChannel.getFileChannel()),
					zarrDatasetAttributes,
					dataBlock);
		}
	}

	@Override
	public boolean deleteBlock( final String pathName, final long... gridPosition) throws IOException
	{

		final DatasetAttributes datasetAttributes = getDatasetAttributes(pathName);

		final ZarrDatasetAttributes zarrDatasetAttributes;
		if (datasetAttributes instanceof ZarrDatasetAttributes)
			zarrDatasetAttributes = (ZarrDatasetAttributes)datasetAttributes;
		else
			zarrDatasetAttributes = getZArraryAttributes(pathName).getDatasetAttributes();

		final Path path = Paths.get(
				basePath,
				removeLeadingSlash(pathName),
				getZarrDataBlockPath(
						gridPosition,
						dimensionSeparator,
						zarrDatasetAttributes.isRowMajor()).toString());

		if (!Files.exists(path))
			return true;

		try (final N5FSReader.LockedFileChannel lockedChannel = N5FSReader.LockedFileChannel.openForWriting(path)) {
			Files.delete(path);
		}

		return !Files.exists(path);
	}

	@Override
	public boolean remove() throws IOException
	{

		return remove("/");
	}

	@Override
	public boolean remove(final String pathName) throws IOException
	{

		final Path path = Paths.get(basePath, pathName);
		if ( Files.exists(path))
			try (final Stream< Path > pathStream = Files.walk(path)) {
				pathStream.sorted( Comparator.reverseOrder()).forEach(
						childPath -> {
							if ( Files.isRegularFile(childPath)) {
								try (final N5FSReader.LockedFileChannel channel = N5FSReader.LockedFileChannel.openForWriting(childPath)) {
									Files.delete(childPath);
								} catch (final IOException e) {
									e.printStackTrace();
								}
							} else
								try {
									Files.delete(childPath);
								} catch (final IOException e) {
									e.printStackTrace();
								}
						});
			}

		return !Files.exists(path);
	}

	/**
	 * This is a copy of {@link Files#createDirectories(Path, FileAttribute...)}
	 * that follows symlinks.
	 *
	 * Workaround for https://bugs.openjdk.java.net/browse/JDK-8130464
	 *
     * Creates a directory by creating all nonexistent parent directories first.
     * Unlike the {@link #createDirectory createDirectory} method, an exception
     * is not thrown if the directory could not be created because it already
     * exists.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the nonexistent
     * directories. Each file attribute is identified by its {@link
     * FileAttribute#name name}. If more than one attribute of the same name is
     * included in the array then all but the last occurrence is ignored.
     *
     * <p> If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @param   dir
     *          the directory to create
     *
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the directory
     *
     * @return  the directory
     *
     * @throws UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws FileAlreadyExistsException
     *          if {@code dir} exists but is not a directory <i>(optional specific
     *          exception)</i>
     * @throws IOException
     *          if an I/O error occurs
     * @throws SecurityException
     *          in the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked prior to attempting to create a directory and
     *          its {@link SecurityManager#checkRead(String) checkRead} is
     *          invoked for each parent directory that is checked. If {@code
     *          dir} is not an absolute path then its {@link Path#toAbsolutePath
     *          toAbsolutePath} may need to be invoked to get its absolute path.
     *          This may invoke the security manager's {@link
     *          SecurityManager#checkPropertyAccess(String) checkPropertyAccess}
     *          method to check access to the system property {@code user.dir}
     */
    private static Path createDirectories( Path dir, final FileAttribute<?>... attrs)
        throws IOException
    {
        // attempt to create the directory
        try {
            createAndCheckIsDirectory(dir, attrs);
            return dir;
        } catch (final FileAlreadyExistsException x) {
            // file exists and is not a directory
            throw x;
        } catch (final IOException x) {
            // parent may not exist or other reason
        }
        SecurityException se = null;
        try {
            dir = dir.toAbsolutePath();
        } catch (final SecurityException x) {
            // don't have permission to get absolute path
            se = x;
        }
        // find a decendent that exists
        Path parent = dir.getParent();
        while (parent != null) {
            try {
            	parent.getFileSystem().provider().checkAccess(parent);
                break;
            } catch (final NoSuchFileException x) {
                // does not exist
            }
            parent = parent.getParent();
        }
        if (parent == null) {
            // unable to find existing parent
            if (se == null) {
                throw new FileSystemException(dir.toString(), null,
                    "Unable to determine if root directory exists");
            } else {
                throw se;
            }
        }

        // create directories
        Path child = parent;
        for (final Path name: parent.relativize(dir)) {
            child = child.resolve(name);
            createAndCheckIsDirectory(child, attrs);
        }
        return dir;
    }

    /**
     * This is a copy of {@link Files#createAndCheckIsDirectory(Path, FileAttribute...)}
     * that follows symlinks.
     *
     * Workaround for https://bugs.openjdk.java.net/browse/JDK-8130464
     *
     * Used by createDirectories to attempt to create a directory. A no-op
     * if the directory already exists.
     */
    private static void createAndCheckIsDirectory(final Path dir,
                                                  final FileAttribute<?>... attrs)
        throws IOException
    {
        try {
            Files.createDirectory(dir, attrs);
        } catch (final FileAlreadyExistsException x) {
            if (!Files.isDirectory(dir))
                throw x;
        }
    }

    /**
	 * Removes the trailing slash from a given path and returns the corrected path.
	 *
	 * @param pathName
	 * @return
	 */
	protected static String removeTrailingSlash( final String pathName) {

		return pathName.endsWith("/") || pathName.endsWith("\\") ? pathName.substring(0, pathName.length() - 1) : pathName;
	}
}
