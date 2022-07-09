package mobie3.viewer.source;

import mobie3.viewer.segment.LabelToSegmentMapper;
import mobie3.viewer.segment.SegmentProvider;
import mobie3.viewer.table.AnnotationTableModel;
import mobie3.viewer.table.SegmentRow;
import mobie3.viewer.table.TableSawSegmentationTableModel;
import net.imglib2.type.numeric.integer.IntType;


public class AnnotatedLabelMaskImage< S extends SegmentRow > implements AnnotatedImage< S >
{
	private final Image< IntType > labelMask;
	private AnnotationTableModel< S > tableModel;
	private SourcePair< AnnotationType< S > > sourcePair;

	// TODO:
	//   - probably give the tableModel in the constructor!
	//   this may make this class much more flexible
	//   Check how this works with actually creating the
	//   image.
	//   - probably also give a Map< SegmentProperty, ColumnName (String) >
	public AnnotatedLabelMaskImage( Image< IntType > labelMask, AnnotationTableModel< S > tableModel )
	{
		this.labelMask = labelMask;
		this.tableModel = tableModel;
	}

	@Override
	public SourcePair< AnnotationType< S > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			SegmentProvider< S > segmentProvider = new LabelToSegmentMapper( tableModel );
			final AnnotatedLabelMaskSource s = new AnnotatedLabelMaskSource<>( labelMask.getSourcePair().getSource(), segmentProvider );
			final VolatileAnnotatedLabelMaskSource vs = new VolatileAnnotatedLabelMaskSource<>( labelMask.getSourcePair().getVolatileSource(), segmentProvider );
			sourcePair = new DefaultSourcePair<>( s, vs );
		}

		return sourcePair;
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
