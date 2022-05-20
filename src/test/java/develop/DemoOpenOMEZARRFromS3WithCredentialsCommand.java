package develop;

import org.embl.mobie.viewer.command.OpenOMEZARRFromS3WithCredentialsCommand;

public class DemoOpenOMEZARRFromS3WithCredentialsCommand
{
	public static void main( String[] args )
	{
		final OpenOMEZARRFromS3WithCredentialsCommand command = new OpenOMEZARRFromS3WithCredentialsCommand();
		command.s3URL = "https://s3.embl.de/comulis/elena/images/bdv-ome-zarr/SXAA03648.ome.zarr";
		command.s3AccessKey = "UYP3FNN3V5F0P86DR2O3";
		command.s3SecretKey = "FIXME";
		command.run();
	}
}
