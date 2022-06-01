/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
		final GenericDialog dialog = new GenericDialog( "" );
		dialog.addStringField( "accessKey", "" );
		dialog.addStringField( "secretKey", "" );
		dialog.showDialog();
		if (!dialog.wasCanceled());
		final String accessKey = dialog.getNextString();
		final String secretKey = dialog.getNextString();


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
