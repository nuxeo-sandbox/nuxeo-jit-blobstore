package org.nuxeo.data.gen.out;

import java.io.ByteArrayInputStream;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

public class S3Writer implements BlobWriter {

	public static final String NAME= "s3:";
	
	protected String bucketName;
	
	protected AWSCredentialsProvider credProvider;

	protected AmazonS3 s3;

    public  void initAWSCredentials(String accessKeyId, String secretKey,
            String sessionToken) {

    	if (accessKeyId!=null && secretKey !=null) {
            if (sessionToken !=null ) {
            	credProvider = new AWSStaticCredentialsProvider(
                        new BasicSessionCredentials(accessKeyId, secretKey, sessionToken));
            } else {
            	credProvider= new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey));
            }
        } else {
        	credProvider = new DefaultAWSCredentialsProviderChain();
        }
    }

    
	protected void initClient() {
		
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		String region = new DefaultAwsRegionProviderChain().getRegion();
		
		clientConfiguration.setMaxConnections(500);
		clientConfiguration.setUseGzip(false);
		clientConfiguration.setUseTcpKeepAlive(true);

		AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder.standard()
                .withCredentials(credProvider)
                .withClientConfiguration(clientConfiguration)
                .withRegion(region);
		
		s3 = s3Builder.build();
		
	}

	public S3Writer (String bucketName) {
		this(bucketName, null, null, null);
	}	

	public S3Writer (String bucketName, String accessKeyId, String secretKey,
            String sessionToken) {
		this.bucketName = bucketName;		
		initAWSCredentials(accessKeyId, secretKey, sessionToken);
		initClient();
	}	
			
	@Override
	public void write(byte[] data, String digest) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		//meta.setContentMD5(digest);
		
		PutObjectRequest put = new PutObjectRequest(bucketName, digest, new ByteArrayInputStream(data), meta);
		PutObjectResult res = s3.putObject(put);
	}
	
	@Override
	public void flush() {
		// NOP
	}

}
