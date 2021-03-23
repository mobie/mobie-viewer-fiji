package de.embl.cba.mobie2.select;

import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.HashMap;

public class SelectionModelAndLabelAdapter< T extends ImageSegment >
{
	private final SelectionModel< T > selectionModel;
	private final HashMap< LabelFrameAndImage, T > adapter;

	public SelectionModelAndLabelAdapter( SelectionModel< T > selectionModel, HashMap< LabelFrameAndImage, T> adapter )
	{
		this.selectionModel = selectionModel;
		this.adapter = adapter;
	}

	public SelectionModel< T > getSelectionModel()
	{
		return selectionModel;
	}

	public HashMap< LabelFrameAndImage, T > getAdapter()
	{
		return adapter;
	}
}
