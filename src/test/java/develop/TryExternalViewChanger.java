package develop;

import org.embl.mobie.MoBIE;

public class TryExternalViewChanger
{
    public static void main( String[] args )
    {
        MoBIE moBIE = MoBIE.getInstance();
        moBIE.getViewManager().getBigVolumeViewer();
        moBIE.getViewManager().show( "t0" );
    }
}
