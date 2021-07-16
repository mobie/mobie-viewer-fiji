package develop;

import com.glencoesoftware.bioformats2raw.Converter;
import picocli.CommandLine;


public class DevelopOmeZarrWriting {
    public static void main( String[] args )
    {
        // String[] commandlineArgs = { "--help" };
        // null compression as otherwise needs blosc/zlib installed
        String[] commandlineArgs = { "--compression=null", "C:\\Users\\meechan\\Documents\\test_images\\zebrafish\\0B51F8B46C_8bit_lynEGFP.tif",
                "C:\\Users\\meechan\\Documents\\temp\\test_zarr_writing\\test.zarr"};
        int exitCode = new CommandLine( new Converter() ).execute( commandlineArgs );
    }
}
