package mobie3.viewer.table;

public interface AnnotationTableAdapter< A extends Row >
{
	int rowIndex( A annotation );
	A getAnnotation( int rowIndex );
}
