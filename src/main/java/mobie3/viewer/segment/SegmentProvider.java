package mobie3.viewer.segment;

public interface SegmentProvider< S extends Segment >
{
	S getSegment( int label, int t, String imageId );
	S createVariable();
}
