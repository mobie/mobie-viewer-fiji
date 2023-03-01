package develop;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.util.BdvStackSource;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;

import java.awt.*;

public class DebugBdvOverlay
{
	public static void main( String[] args )
	{
		// https://imagesc.zulipchat.com/#narrow/stream/327326-BigDataViewer/topic/BdvOverlay.20and.20Timepoints

		BdvStackSource< ByteType > bdvStackSource = BdvFunctions.show( ArrayImgs.bytes(2, 2, 2), "image" );
		BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		BdvFunctions.showOverlay( new BdvOverlay()
		{
			@Override
			protected void draw( Graphics2D g )
			{
			}
		}, "overlay", new BdvOptions().addTo( bdvHandle ) );

	}
}
