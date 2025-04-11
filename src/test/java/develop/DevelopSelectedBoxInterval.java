package develop;

import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class DevelopSelectedBoxInterval
{
    public static void main(String[] args) {
        // Define the target RealInterval with min and max bounds
        double minX = 2.0, minY = 3.0, minZ = 4.0;
        double maxX = 5.0, maxY = 6.0, maxZ = 7.0;

        RealInterval targetInterval = new FinalRealInterval(
                new double[]{minX, minY, minZ},
                new double[]{maxX, maxY, maxZ});
        // Create the AffineTransform3D
        AffineTransform3D transform = new AffineTransform3D();

        // Calculate scale factors
        double scaleX = maxX - minX;
        double scaleY = maxY - minY;
        double scaleZ = maxZ - minZ;

        // Set scaling
        transform.set(scaleX, 0, 0, minX,
                0, scaleY, 0, minY,
                0, 0, scaleZ, minZ);

        // Use estimateBounds to verify the transformation
        RealInterval unitInterval = new FinalRealInterval(
                new double[]{0.0, 0.0, 0.0},
                new double[]{1.0, 1.0, 1.0});
        RealInterval transformedBounds = transform.estimateBounds(unitInterval);

        // Print the transformed bounds
        System.out.println("Transformed bounds: min = (" +
                transformedBounds.realMin(0) + ", " +
                transformedBounds.realMin(1) + ", " +
                transformedBounds.realMin(2) + "), max = (" +
                transformedBounds.realMax(0) + ", " +
                transformedBounds.realMax(1) + ", " +
                transformedBounds.realMax(2) + ")");
    }
}
