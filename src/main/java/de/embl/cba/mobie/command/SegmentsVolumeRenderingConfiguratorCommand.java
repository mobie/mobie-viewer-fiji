package de.embl.cba.mobie.command;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.color.LabelConverter;
import de.embl.cba.mobie.volume.SegmentsVolumeViewer;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Configure segments volume rendering")
public class SegmentsVolumeRenderingConfiguratorCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Resolution X [um]")
	double sx;

	@Parameter ( label = "Resolution Y [um]")
	double sy;

	@Parameter ( label = "Resolution Z [um]")
	double sz;

	@Override
	public void run()
	{
		setVoxelSpacing( sourceAndConverters, new double[]{ sx, sy, sz } );
	}

	public static void setVoxelSpacing( SourceAndConverter[] sourceAndConverters, double[] voxelSpacing )
	{
		final SourceAndConverterService sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			final SegmentsVolumeViewer volumeViewer = ( SegmentsVolumeViewer ) sacService.getMetadata( sourceAndConverter, SegmentsVolumeViewer.VOLUME_VIEW );
			if ( volumeViewer != null )
			{
				volumeViewer.setVoxelSpacing( voxelSpacing );
			}
		}
	}
}
