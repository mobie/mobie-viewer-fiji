package mobie3.viewer.source;

import mobie3.viewer.table.AnnotationTableModel;
import mobie3.viewer.table.SegmentRow;
import mobie3.viewer.table.SegmentationTableModel;
import net.imglib2.type.numeric.integer.IntType;

// currently TableSaw based
public class SegmentationImage implements AnnotatedImage< SegmentRow >
{
	private final Image< IntType > labelMask;
	private final String tableDir;

	// TODO: tableDir or list of TablePaths?
	public SegmentationImage( Image< IntType > labelMask, String tableDir )
	{
		this.labelMask = labelMask;
		this.tableDir = tableDir;
		new SegmentationTableModel<>()
	}

	@Override
	public SourcePair< AnnotationType< SegmentRow > > getSourcePair()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return null;
	}

	@Override
	public AnnotationTableModel< SegmentRow > getTable()
	{
		return null;
	}
}
