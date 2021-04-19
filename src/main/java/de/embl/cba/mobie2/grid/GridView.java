package de.embl.cba.mobie2.grid;

import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.color.LutFactory;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.source.SegmentationSource;
import de.embl.cba.mobie2.transform.GridSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.mobie.utils.Utils.createAnnotatedImageSegmentsFromTableFile;

public class GridView
{
	private final MoBIEColoringModel< TableRowImageSegment > coloringModel;
	private final DefaultSelectionModel< TableRowImageSegment > selectionModel;

	public GridView( MoBIE2 moBIE2, String name, String tableDataLocation )
	{
		coloringModel = new MoBIEColoringModel< TableRowImageSegment >( ColoringLuts.GLASBEY );
		selectionModel = new DefaultSelectionModel< TableRowImageSegment >();
		coloringModel.setSelectionModel( selectionModel );

		final List< TableRowImageSegment > segmentsFromTableFile = createAnnotatedImageSegmentsFromTableFile(
					moBIE2.getDefaultTableLocation( tableDataLocation ),
					getImageName( name ) );

		final TableViewer< TableRowImageSegment > tableViewer = new TableViewer<>( segmentsFromTableFile, selectionModel, coloringModel, getImageName( name ) ).show();
	}

	private String getImageName( String name )
	{
		return name + "-grid-features";
	}


}
