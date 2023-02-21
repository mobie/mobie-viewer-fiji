package org.embl.mobie.lib.hcs;

import mpicbg.spim.data.SpimDataException;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.ImageDataSource;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.transformation.MergedGridTransformation;
import spimdata.util.Displaysettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class HCSDataSetter
{
	/**
	 * Adds the content of the {@code hcsPlate} to the {@code dataset}.
	 *
	 * @param hcsPlate
	 * 					a HCSPlate
	 * @param dataset
	 * 					the current MoBIE dataset
	 */
	public void addPlateToDataset( HCSPlate hcsPlate, Dataset dataset )
	{
		final Set< String > channels = hcsPlate.getChannels();

		for ( String channel : channels )
		{
			final Set< String > wells = hcsPlate.getWells( channel );

			for ( String well : wells )
			{
				final String channelWell = "c" + channel + "-" + well;

				final MergedGridTransformation grid = new MergedGridTransformation();
				grid.sources = new ArrayList<>();
				grid.positions = new ArrayList<>();
				grid.mergedGridSourceName = channelWell;

				final Set< String > sites = hcsPlate.getSites( channel, well );
				for ( String site : sites )
				{
					final StorageLocation storageLocation = new StorageLocation();
					storageLocation.absolutePath = hcsPlate.getPath( channel, well, site );
					storageLocation.channel = 0;
					System.out.println( site + ":" + storageLocation.absolutePath );
					final ImageDataSource imageDataSource = new ImageDataSource( hcsPlate.getSiteKey( channel, well, site ), ImageDataFormat.BioFormats, storageLocation );
					dataset.addDataSource( imageDataSource );
					grid.sources.add( imageDataSource.getName() );
					grid.positions.add( hcsPlate.getSiteGridPosition( channel, well, site ) );
				}

				// Add well view for testing
				String color = "White";
				double[] contrastLimits = new double[]{0, 1000}; // TODO
				final ImageDisplay< ? > imageDisplay = new ImageDisplay<>( channelWell, Arrays.asList( channelWell ), color, contrastLimits );
				final View view = new View( channelWell, "well", Arrays.asList( imageDisplay ), Arrays.asList( grid ), true );
				dataset.views.put( view.getName(), view );
			}
		}
	}

}
