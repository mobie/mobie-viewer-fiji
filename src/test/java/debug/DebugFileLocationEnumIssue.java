package debug;

import org.embl.mobie.lib.io.FileLocation;

public class DebugFileLocationEnumIssue
{
    public static void main( String[] args )
    {
        FileLocation fileLocation = FileLocation.fromString( FileLocation.CurrentProject.toString() );
        System.out.println( fileLocation );
        fileLocation = FileLocation.fromString( "aaa ");
        System.out.println( fileLocation );

    }
}
