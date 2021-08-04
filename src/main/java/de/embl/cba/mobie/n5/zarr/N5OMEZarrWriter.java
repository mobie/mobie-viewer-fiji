package de.embl.cba.mobie.n5.zarr;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;

public class N5OMEZarrWriter extends N5ZarrWriter {

    public N5OMEZarrWriter(String basePath, GsonBuilder gsonBuilder, String dimensionSeparator, boolean mapN5DatasetAttributes) throws IOException {
        super(basePath, gsonBuilder, dimensionSeparator, mapN5DatasetAttributes);
    }

    public N5OMEZarrWriter(String basePath, GsonBuilder gsonBuilder, String dimensionSeparator) throws IOException {
        super(basePath, gsonBuilder, dimensionSeparator);
    }

    public N5OMEZarrWriter(String basePath, String dimensionSeparator, boolean mapN5DatasetAttributes) throws IOException {
        super(basePath, dimensionSeparator, mapN5DatasetAttributes);
    }

    public N5OMEZarrWriter(String basePath, boolean mapN5DatasetAttributes) throws IOException {
        super(basePath, mapN5DatasetAttributes);
    }

    public N5OMEZarrWriter(String basePath, GsonBuilder gsonBuilder) throws IOException {
        super(basePath, gsonBuilder);
    }

    public N5OMEZarrWriter(String basePath) throws IOException {
        super(basePath);
    }

    @Override
    public void createDataset(
            final String pathName,
            final DatasetAttributes datasetAttributes) throws IOException
    {
        int lastSlashIndex = removeTrailingSlash(pathName).lastIndexOf("/");

        if ( lastSlashIndex != -1 ) {
            /* create parent groups */
            final String parentGroup = pathName.substring(0, removeTrailingSlash(pathName).lastIndexOf('/'));
            if (!parentGroup.equals(""))
                createGroup(parentGroup);
        }

        final Path path = Paths.get(basePath, pathName);
        createDirectories(path);

        setDatasetAttributes(pathName, datasetAttributes);
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
}
