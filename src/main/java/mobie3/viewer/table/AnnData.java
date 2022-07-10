package mobie3.viewer.table;

public interface AnnData< R extends Row >
{
	AnnotationTableModel< R > getTable();
	AnnData< R > transform();
}
