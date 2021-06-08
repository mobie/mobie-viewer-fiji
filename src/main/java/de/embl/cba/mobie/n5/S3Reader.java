package de.embl.cba.mobie.n5;

import ij.IJ;

public abstract class S3Reader {
    protected static boolean logChunkLoading;

    protected final String serviceEndpoint;
    protected final String signingRegion;
    protected final String bucketName;

    public S3Reader(String serviceEndpoint, String signingRegion, String bucketName) {
        this.serviceEndpoint = serviceEndpoint;
        this.signingRegion = signingRegion;
        this.bucketName = bucketName;
    }

    public static void setLogChunkLoading(boolean logChunkLoading) {
        S3Reader.logChunkLoading = logChunkLoading;
        if (logChunkLoading) IJ.run("Console");
    }
}
