package de.embl.cba.mobie2.select;

import de.embl.cba.mobie2.segment.SegmentAdapter;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.select.SelectionModel;

public class SelectionModelAndSegmentConverter< T extends ImageSegment >
{
	private final SelectionModel< T > selectionModel;
	private final SegmentAdapter< T > converter;

	public SelectionModelAndSegmentConverter( SelectionModel< T > selectionModel, SegmentAdapter< T > converter )
	{
		this.selectionModel = selectionModel;
		this.converter = converter;
	}

	public SelectionModel< T > getSelectionModel()
	{
		return selectionModel;
	}

	public SegmentAdapter< T > getConverter()
	{
		return converter;
	}
}
