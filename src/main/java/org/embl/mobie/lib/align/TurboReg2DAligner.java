/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.align;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.transform.Transform;

import java.lang.reflect.Method;

public class TurboReg2DAligner
{
    private final ImagePlus source;
    private final ImagePlus target;
    private final Transform transformationType;
    private double[][] transformationMatrix;

    public TurboReg2DAligner( ImagePlus source, ImagePlus target, Transform transformationType )
    {
        this.source = source;
        this.target = target;
        this.transformationType = transformationType;
    }

    public boolean run( Boolean showIntermediates )
    {
        int width = source.getWidth();
        int height = source.getHeight();

        // Code adapted from 
        // https://github.com/fiji-BIG/StackReg/blob/master/src/main/java/StackReg_.java#L1021C1-L1104C20
        final FileSaver sourceFile = new FileSaver(source);
        final String sourcePathAndFileName = IJ.getDirectory("temp") + "source.tif";
        sourceFile.saveAsTiff(sourcePathAndFileName);
        final FileSaver targetFile = new FileSaver(target);
        final String targetPathAndFileName = IJ.getDirectory("temp") + "target.tif";
        targetFile.saveAsTiff(targetPathAndFileName);

        Object turboReg;
        Method method;
        switch (transformationType) {
            case Translation: {
                turboReg = IJ.runPlugIn("TurboReg_", "-align"
                        + " -file " + sourcePathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -file " + targetPathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -translation"
                        + " " + (width / 2) + " " + (height / 2)
                        + " " + (width / 2) + " " + (height / 2)
                        + " -hideOutput"
                );
                break;
            }
            case Rigid: {
                turboReg = IJ.runPlugIn("TurboReg_", "-align"
                        + " -file " + sourcePathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -file " + targetPathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -rigidBody"
                        + " " + (width / 2) + " " + (height / 2)
                        + " " + (width / 2) + " " + (height / 2)
                        + " " + (width / 2) + " " + (height / 4)
                        + " " + (width / 2) + " " + (height / 4)
                        + " " + (width / 2) + " " + ((3 * height) / 4)
                        + " " + (width / 2) + " " + ((3 * height) / 4)
                        + " -hideOutput"
                );
                break;
            }
            case Similarity: {
                turboReg = IJ.runPlugIn("TurboReg_", "-align"
                        + " -file " + sourcePathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -file " + targetPathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -scaledRotation"
                        + " " + (width / 4) + " " + (height / 2)
                        + " " + (width / 4) + " " + (height / 2)
                        + " " + ((3 * width) / 4) + " " + (height / 2)
                        + " " + ((3 * width) / 4) + " " + (height / 2)
                        + " -hideOutput"
                );
                break;
            }
            case Affine: {
                turboReg = IJ.runPlugIn("TurboReg_", "-align"
                        + " -file " + sourcePathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -file " + targetPathAndFileName
                        + " 0 0 " + (width - 1) + " " + (height - 1)
                        + " -affine"
                        + " " + (width / 2) + " " + (height / 4)
                        + " " + (width / 2) + " " + (height / 4)
                        + " " + (width / 4) + " " + ((3 * height) / 4)
                        + " " + (width / 4) + " " + ((3 * height) / 4)
                        + " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
                        + " " + ((3 * width) / 4) + " " + ((3 * height) / 4)
                        + " -hideOutput"
                );
                break;
            }
            default: {
                IJ.error("Unsupported transformation: " + transformationType);
                return false;
            }
        }
        try
        {
            method = turboReg.getClass().getMethod( "getSourcePoints", null );
            double[][] sourcePoints = ( ( double[][] ) method.invoke( turboReg, null ) );
            method = turboReg.getClass().getMethod( "getTargetPoints", null );
            double[][] targetPoints = ( ( double[][] ) method.invoke( turboReg, null ) );
            // If this is a licensing issue, we could probably use methods in BigWarp
            // or mpicbg to compute the transformation from a set of points.
            transformationMatrix = getTransformationMatrix( targetPoints, sourcePoints, transformationType );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
        return true;
    }

    // Code copied and slightly adapted from
    // https://github.com/fiji-BIG/StackReg/blob/master/src/main/java/StackReg_.java#L1021C1-L1104C20
    private double[][] getTransformationMatrix( double[][] fromCoord, double[][] toCoord, Transform transformation) {
        double[][] matrix;
        matrix = new double[3][3];
        double[][] a;
        double[] v;
        int i;
        int j;
        label81:
        switch (transformation) {
            case Translation:
                matrix[0][0] = 1.0;
                matrix[0][1] = 0.0;
                matrix[0][2] = toCoord[0][0] - fromCoord[0][0];
                matrix[1][0] = 0.0;
                matrix[1][1] = 1.0;
                matrix[1][2] = toCoord[0][1] - fromCoord[0][1];
                break;
            case Rigid:
                double angle = Math.atan2(fromCoord[2][0] - fromCoord[1][0], fromCoord[2][1] - fromCoord[1][1]) - Math.atan2(toCoord[2][0] - toCoord[1][0], toCoord[2][1] - toCoord[1][1]);
                double c = Math.cos(angle);
                double s = Math.sin(angle);
                matrix[0][0] = c;
                matrix[0][1] = -s;
                matrix[0][2] = toCoord[0][0] - c * fromCoord[0][0] + s * fromCoord[0][1];
                matrix[1][0] = s;
                matrix[1][1] = c;
                matrix[1][2] = toCoord[0][1] - s * fromCoord[0][0] - c * fromCoord[0][1];
                break;
            case Similarity:
                a = new double[3][3];
                v = new double[3];
                a[0][0] = fromCoord[0][0];
                a[0][1] = fromCoord[0][1];
                a[0][2] = 1.0;
                a[1][0] = fromCoord[1][0];
                a[1][1] = fromCoord[1][1];
                a[1][2] = 1.0;
                a[2][0] = fromCoord[0][1] - fromCoord[1][1] + fromCoord[1][0];
                a[2][1] = fromCoord[1][0] + fromCoord[1][1] - fromCoord[0][0];
                a[2][2] = 1.0;
                this.invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[0][1] - toCoord[1][1] + toCoord[1][0];

                for(i = 0; i < 3; ++i) {
                    matrix[0][i] = 0.0;

                    for(j = 0; j < 3; ++j) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }

                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[1][0] + toCoord[1][1] - toCoord[0][0];
                i = 0;

                while(true) {
                    if (i >= 3) {
                        break label81;
                    }

                    matrix[1][i] = 0.0;

                    for(j = 0; j < 3; ++j) {
                        matrix[1][i] += a[i][j] * v[j];
                    }

                    ++i;
                }
            case Affine:
                a = new double[3][3];
                v = new double[3];
                a[0][0] = fromCoord[0][0];
                a[0][1] = fromCoord[0][1];
                a[0][2] = 1.0;
                a[1][0] = fromCoord[1][0];
                a[1][1] = fromCoord[1][1];
                a[1][2] = 1.0;
                a[2][0] = fromCoord[2][0];
                a[2][1] = fromCoord[2][1];
                a[2][2] = 1.0;
                this.invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[2][0];

                for(i = 0; i < 3; ++i) {
                    matrix[0][i] = 0.0;

                    for(j = 0; j < 3; ++j) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }

                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[2][1];
                i = 0;

                while(true) {
                    if (i >= 3) {
                        break label81;
                    }

                    matrix[1][i] = 0.0;

                    for(j = 0; j < 3; ++j) {
                        matrix[1][i] += a[i][j] * v[j];
                    }

                    ++i;
                }
            default:
                IJ.error("Unexpected transformation");
        }

        matrix[2][0] = 0.0;
        matrix[2][1] = 0.0;
        matrix[2][2] = 1.0;
        return matrix;
    }

    private void invertGauss(double[][] matrix) {
        int n = matrix.length;
        double[][] inverse = new double[n][n];

        int j;
        double max;
        double absMax;
        int k;
        for(j = 0; j < n; ++j) {
            max = matrix[j][0];
            absMax = Math.abs(max);

            for(k = 0; k < n; ++k) {
                inverse[j][k] = 0.0;
                if (absMax < Math.abs(matrix[j][k])) {
                    max = matrix[j][k];
                    absMax = Math.abs(max);
                }
            }

            inverse[j][j] = 1.0 / max;

            for(k = 0; k < n; ++k) {
                matrix[j][k] /= max;
            }
        }

        for(j = 0; j < n; ++j) {
            max = matrix[j][j];
            absMax = Math.abs(max);
            k = j;

            int i;
            for(i = j + 1; i < n; ++i) {
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                    k = i;
                }
            }

            if (k != j) {
                double[] partialLine = new double[n - j];
                double[] fullLine = new double[n];
                System.arraycopy(matrix[j], j, partialLine, 0, n - j);
                System.arraycopy(matrix[k], j, matrix[j], j, n - j);
                System.arraycopy(partialLine, 0, matrix[k], j, n - j);
                System.arraycopy(inverse[j], 0, fullLine, 0, n);
                System.arraycopy(inverse[k], 0, inverse[j], 0, n);
                System.arraycopy(fullLine, 0, inverse[k], 0, n);
            }

            for(k = 0; k <= j; ++k) {
                inverse[j][k] /= max;
            }

            for(k = j + 1; k < n; ++k) {
                matrix[j][k] /= max;
                inverse[j][k] /= max;
            }

            for(i = j + 1; i < n; ++i) {
                for(k = 0; k <= j; ++k) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }

                for(k = j + 1; k < n; ++k) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }

        for(j = n - 1; 1 <= j; --j) {
            for(int i = j - 1; 0 <= i; --i) {
                for(k = 0; k <= j; ++k) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for(k = j + 1; k < n; ++k) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }

        for(j = 0; j < n; ++j) {
            System.arraycopy(inverse[j], 0, matrix[j], 0, n);
        }

    }

    public AffineTransform3D getAlignmentTransform()
    {
        AffineTransform3D turboRegTransform = new AffineTransform3D();
        double[][] a = transformationMatrix;
        turboRegTransform.set(
                a[0][0], a[0][1], 0, a[0][2],
                a[1][0], a[1][1], 0, a[1][2],
                0, 0, 1, 0);
        return turboRegTransform;
    }
}
