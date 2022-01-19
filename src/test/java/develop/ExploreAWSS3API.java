package develop;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import ij.gui.GenericDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ExploreAWSS3API
{
	public static void main( String[] args ) throws IOException
	{
		// Get credentials
//		final GenericDialog dialog = new GenericDialog( "" );
//		dialog.addStringField( "accessKey", "" );
//		dialog.addStringField( "secretKey", "" );
//		dialog.showDialog();
//		if (!dialog.wasCanceled());
//		final String accessKey = dialog.getNextString();
//		final String secretKey = dialog.getNextString();

		// Create AWS service
		String accessKey = "cbb-bigdata";
		String secretKey = "UZUTutgnW7";

		accessKey = "UYP3FNN3V5F0P86DR2O3";
		secretKey = "3EL7Czzg0vVwx2L4v27GQiX0Ct1GkMHS+tbcJR3D";

		final String serviceEndpoint = "https://s3.embl.de";
		final String signingRegion = "us-west-2";

		final AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration( serviceEndpoint, signingRegion );

		final BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		final AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
		final AmazonS3 s3 = AmazonS3ClientBuilder
				.standard()
				.withCredentials(credentialsProvider)
				.withEndpointConfiguration(endpointConfiguration)
				.withPathStyleAccessEnabled(true)
				.build();

		// List buckets
		List<Bucket> buckets = s3.listBuckets();
		System.out.println("Your buckets are:");
		for (Bucket bucket : buckets) {
			System.out.println("* " + bucket.getName());
		}

		// List objects in bucket
		ListObjectsV2Request request = new ListObjectsV2Request()
				.withBucketName("comulis")
				.withPrefix("")
				.withDelimiter("/");
		final ListObjectsV2Result result = s3.listObjectsV2( request );
		final List< S3ObjectSummary > objectSummaries = result.getObjectSummaries();

		for (S3ObjectSummary objectSummary : objectSummaries) {
			System.out.printf(" - %s (size: %d)\n", objectSummary.getKey(), objectSummary.getSize());
		}

		// Download object
		final S3Object s3Object = s3.getObject( new GetObjectRequest( "comulis", "test.txt" ) );
		System.out.println("Content-Type: " + s3Object.getObjectMetadata().getContentType());
		System.out.println("Content: ");
		displayTextInputStream(s3Object.getObjectContent());
	}

	private static void displayTextInputStream( InputStream input) throws IOException
	{
		// Read the text input stream one line at a time and display each line.
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
		System.out.println();
	}
}
