package org.embl.mobie.viewer.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.integer.IntType;
import org.embl.mobie.viewer.annotate.AnnotatedMaskAdapter;
import org.embl.mobie.viewer.annotate.AnnotatedMaskTableRow;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.bdv.view.AnnotatedMaskSliceView;
import org.embl.mobie.viewer.bdv.view.AnnotatedRegionSliceView;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.*;

// Even though within the rest of the code we call things now AnnotatedMask*
// the name of this class for now remains AnnotatedSourceDisplay
// in order to stay consistent with the nomenclature in dataset.json
public class AnnotatedSourceDisplay extends AnnotatedRegionDisplay< AnnotatedMaskTableRow >
{
	// Serialization
	protected Map< String, List< String > > sources;
	protected List< String > selectedAnnotationIds;
	protected Map< TableDataFormat, StorageLocation > tableData;

	// Runtime
	public transient AnnotatedMaskAdapter annotatedMaskAdapter;
	public transient AnnotatedMaskSliceView sliceView;

	// Getters for the serialised fields
	public List< String > getSelectedAnnotationIds()
	{
		return selectedAnnotationIds;
	}

	public String getTableDataFolder( TableDataFormat tableDataFormat )
	{
		return tableData.get( tableDataFormat ).relativePath;
	}

	public Map< String, List< String > > getAnnotationIdToSources()
	{
		return sources;
	}

	@Override
	public List< String > getSources()
	{
		final ArrayList< String > allSources = new ArrayList<>();
		for ( List< String > sources : this.sources.values() )
			allSources.addAll( sources );
		return allSources;
	}

	@Override
	public AnnotatedRegionSliceView< ? > getSliceView()
	{
		return sliceView;
	}

	// Needed for Gson
	public AnnotatedSourceDisplay() {}

	// TODO: Looks like we do not need it? Maybe for the interactive grid view?
	public AnnotatedSourceDisplay( String name, double opacity, Map< String, List< String > > sources, String lut, String colorByColumn, Double[] valueLimits, List< String > selectedSegmentIds, boolean showScatterPlot, String[] scatterPlotAxes, List< String > tables )
	{
		this.name = name;
		this.opacity = opacity;
		this.sources = sources;
		this.lut = lut;
		this.colorByColumn = colorByColumn;
		this.valueLimits = valueLimits;
		this.selectedAnnotationIds = selectedSegmentIds;
		this.showScatterPlot = showScatterPlot;
		this.scatterPlotAxes = scatterPlotAxes;
		this.tables = tables;
	}

	/**
	 * Create a serializable copy
	 *
	 * @param annotatedSourceDisplay
	 */
	public AnnotatedSourceDisplay( AnnotatedSourceDisplay annotatedSourceDisplay )
	{
		fetchCurrentSettings( annotatedSourceDisplay );

		this.sources = new HashMap<>();
		this.sources.putAll( annotatedSourceDisplay.sources );

		Set< AnnotatedMaskTableRow > currentSelectedRows = annotatedSourceDisplay.selectionModel.getSelected();
		if ( currentSelectedRows != null && currentSelectedRows.size() > 0 ) {
			ArrayList<String> selectedIds = new ArrayList<>();
			for ( AnnotatedMaskTableRow row : currentSelectedRows ) {
				selectedIds.add( row.timePoint() + ";" + row.name() );
			}
			this.selectedAnnotationIds = selectedIds;
		}

		this.tableData = new HashMap<>();
		this.tableData.putAll( annotatedSourceDisplay.tableData );

		if ( annotatedSourceDisplay.sliceView != null ) {
			this.visible = annotatedSourceDisplay.sliceView.isDisplayVisible();
		}
	}

}
