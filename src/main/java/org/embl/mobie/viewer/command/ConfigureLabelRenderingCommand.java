package org.embl.mobie.viewer.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.color.LabelConverter;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Label Rendering")
public class ConfigureLabelRenderingCommand implements BdvPlaygroundActionCommand
{
	public static final String SEGMENT_COLOR = "Label color";
	public static final String SELECTION_COLOR = "Selection color";

	@Parameter
	public BdvHandle bdvh;

	@Parameter
	public SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Show labels as boundaries")
	public boolean showAsBoundary;

	@Parameter ( label = "Boundary thickness", style="format:#.00" )
	public float boundaryThickness = 1.0F;

	@Parameter ( label = "Selected labels coloring", choices = { SEGMENT_COLOR, SELECTION_COLOR } )
	public String coloringMode = SEGMENT_COLOR;

	@Parameter ( label = "Selection color")
	public ColorRGB selectionColor = new ColorRGB(255,255,0);

	@Parameter ( label = "Opacity of non-selected labels" )
	public double opacity = 0.15;

	@Override
	public void run()
	{
		configureBoundaryRendering();

		configureSelectionColor();

		setNonSelectedLabelsOpacity();

		bdvh.getViewerPanel().requestRepaint();
	}

	private void configureSelectionColor()
	{
		if ( coloringMode.equals( SEGMENT_COLOR ) )
		{
			setSelectedSegmentsColor( sourceAndConverters, null );
		}
		else
		{
			final ARGBType argbType = new ARGBType( ARGBType.rgba( selectionColor.getRed(), selectionColor.getGreen(), selectionColor.getBlue(), selectionColor.getAlpha() ) );
			setSelectedSegmentsColor( sourceAndConverters, argbType );
		}
	}

	private void configureBoundaryRendering()
	{
		Arrays.stream( sourceAndConverters ).filter( sac -> SourceHelper.getLabelSource( sac ) != null ).forEach( sac ->
		{
			final LabelSource< ? > labelSource = SourceHelper.getLabelSource( sac );
			labelSource.showAsBoundary( showAsBoundary, boundaryThickness );

			if ( sac.asVolatile() != null )
			{
				( ( LabelSource ) sac.asVolatile().getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
			}
		});
	}

	private void setNonSelectedLabelsOpacity( )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof LabelConverter< ? > )
			{
				final LabelConverter< ? > converter = ( LabelConverter< ? > ) sourceAndConverter.getConverter();

				converter.getColoringModel().setOpacityNotSelected( opacity );
			}
		}
	}

	private void setSelectedSegmentsColor( SourceAndConverter[] sourceAndConverters, ARGBType argbType )
	{
		for ( SourceAndConverter sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getConverter() instanceof LabelConverter< ? > )
			{
				final LabelConverter< ? > converter = ( LabelConverter< ? > ) sourceAndConverter.getConverter();

				converter.getColoringModel().setSelectionColor( argbType );
			}
		}
	}
}
