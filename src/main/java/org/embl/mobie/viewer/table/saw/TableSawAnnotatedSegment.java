package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.volume.MeshTransformer;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.function.Supplier;

public class TableSawAnnotatedSegment extends AbstractTableSawAnnotation implements AnnotatedSegment
{
	private static final String[] idColumns = new String[]{ ColumnNames.LABEL_ID, ColumnNames.TIMEPOINT };

	private final int timePoint;
	private final int labelId;
	private final double[] position;
	private RealInterval boundingBox;
	private float[] mesh;
	private String source;
	private String uuid;

	// TODO only the model is TableSaw specific
	//   all the other code is generic and could be reused to open other tables
	public TableSawAnnotatedSegment(
			TableSawAnnotationTableModel< TableSawAnnotatedSegment > model,
			int rowIndex,
			String source,
			String uuid,
			Integer labelId,
			int timePoint,
			double[] position,
			FinalRealInterval boundingBox )
	{
		super( model, rowIndex );
		this.source = source;
		this.uuid = uuid;
		this.labelId = labelId;
		this.timePoint = timePoint;
		this.position = position;
		this.boundingBox = boundingBox;
	}

	@Override
	public String imageId()
	{
		return source();
	}

	@Override
	public int label()
	{
		return labelId;
	}

	@Override
	public int timePoint()
	{
		return timePoint;
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		return position;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return position[ d ];
	}

	@Override
	public RealInterval boundingBox()
	{
		return boundingBox;
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		this.boundingBox = boundingBox;
	}

	@Override
	public float[] mesh()
	{
		return mesh;
	}

	@Override
	public void setMesh( float[] mesh )
	{
		this.mesh = mesh;
	}

	@Override
	public String uuid()
	{
		return uuid;
	}

	@Override
	public String source()
	{
		return source;
	}

	@Override
	public String[] idColumns()
	{
		return idColumns;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		if ( position != null )
			affineTransform3D.apply( position, position );

		if ( boundingBox != null )
			boundingBox = affineTransform3D.estimateBounds( boundingBox );

		if ( mesh != null )
			mesh = MeshTransformer.transform( mesh, affineTransform3D );
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
