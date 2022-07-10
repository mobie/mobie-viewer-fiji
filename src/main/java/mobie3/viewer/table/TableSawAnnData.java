package mobie3.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;

public class TableSawAnnData implements AnnData< TableSawSegmentRow >
{
	private final TableSawSegmentsTableModel tableModel;

	public TableSawAnnData( TableSawSegmentsTableModel tableModel )
	{
		this.tableModel = tableModel;
	}

	@Override
	public AnnotationTableModel< TableSawSegmentRow > getTable()
	{
		return tableModel;
	}

	@Override
	public AnnData< TableSawSegmentRow > transform()
	{
		return null;
	}

}
