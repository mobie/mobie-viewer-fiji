package mobie3.viewer.table;

public interface AnnotationTableAdapter< A extends Annotation >
{
	int rowIndex( A annotation );
	A getAnnotation( int rowIndex );
}
