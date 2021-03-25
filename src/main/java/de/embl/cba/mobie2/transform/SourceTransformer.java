package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;

import javax.xml.transform.Source;
import java.util.List;

public interface SourceTransformer
{
	List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters  );
}
