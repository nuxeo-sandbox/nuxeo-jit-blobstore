package org.nuxeo.data.gen.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3LS {

	public static final String NAME = "s3:";

	
	protected static AmazonS3 initClient(AWSCredentialsProvider credProvider, String awsEndpoint) {

		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setMaxConnections(500);
		clientConfiguration.setUseGzip(false);
		clientConfiguration.setUseTcpKeepAlive(true);
		
		AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder.standard().withCredentials(credProvider)
				.withClientConfiguration(clientConfiguration);
		
		if (awsEndpoint!=null) {
			EndpointConfiguration epc = new EndpointConfiguration(awsEndpoint, "us-east-1");
			s3Builder = s3Builder.withEndpointConfiguration(epc);
			// see https://docs.aws.amazon.com/snowball/latest/developer-guide/using-adapter.html
			s3Builder = s3Builder.disableChunkedEncoding();
			s3Builder.setPathStyleAccessEnabled(true);
			//s3Builder = s3Builder.enableAccelerateMode();			
		} else {
			String region = new DefaultAwsRegionProviderChain().getRegion();
			s3Builder = s3Builder.withRegion(region);
		}
		return  s3Builder.build();
	}

	
	protected static AmazonS3 initS3(String awsEndpoint) {
		AWSCredentialsProvider credProvider = new DefaultAWSCredentialsProviderChain();
		return  initClient(credProvider, awsEndpoint);
	}
	
	public static void main(String[] args) {
	
		Options options = new Options();
		options.addOption("aws_session", true, "AWS_SESSION_TOKEN");
		options.addOption("aws_endpoint", true, "AWS_ENDPOINT");
		options.addOption("bucket", true, "bucket");
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}
		
		String bucket = cmd.getOptionValue("bucket", "10b-benchmark-blobstore");
		String aws_endpoint = cmd.getOptionValue("aws_endpoint", null);

		AmazonS3 s3Client = initS3(aws_endpoint);
		
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withMaxKeys(1000);
        ListObjectsV2Result result;

        long c=0;
        do {
            result = s3Client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
            	c++;
            	if (objectSummary.getKey().startsWith("arch-")) {
            		System.out.printf(" - %s (size: %d)\n", objectSummary.getKey(), objectSummary.getSize());
            	}                
            }
            String token = result.getNextContinuationToken();
            //System.out.println("Next Continuation Token: " + token);
            req.setContinuationToken(token);
        } while (result.isTruncated());		
        System.out.println("Scanned " + c + " entries");
        
	}
}
