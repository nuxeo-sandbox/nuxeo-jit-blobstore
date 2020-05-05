package org.nuxeo.data.gen.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3Index {

	protected Logger lsLogger;
	protected Logger cmdLogger;
	protected AmazonS3 s3Client;
	protected String bucket;
	
	protected LoggerContext initLogger() {

		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		builder.add(console);

		AppenderComponentBuilder file1 = builder.newAppender("ls", "File");
		file1.addAttribute("fileName", "s3ls.csv");
		file1.addAttribute("filePattern", "s3ls-%d{MM-dd-yy--hh:mm}-%i.csv");
		ComponentBuilder rolloverPolicy = builder.newComponent("Policies")
		        .addComponent(builder.newComponent("OnStartupTriggeringPolicy"));
		file1.addComponent(rolloverPolicy);	

		builder.add(file1);

		// Use Async Logger
		RootLoggerComponentBuilder rootLogger = builder.newAsyncRootLogger(Level.INFO);
		builder.add(rootLogger);

		
		// Use Async Logger
		LoggerComponentBuilder logger1 = builder.newAsyncLogger("lsLogger", Level.DEBUG);
		logger1.add(builder.newAppenderRef("ls"));
		logger1.addAttribute("additivity", false);
		builder.add(logger1);

		LoggerComponentBuilder logger3 = builder.newAsyncLogger("cmdLogger", Level.INFO);
		logger3.add(builder.newAppenderRef("stdout"));
		logger3.addAttribute("additivity", false);
		builder.add(logger3);

		return Configurator.initialize(builder.build());
	}
		
	protected AmazonS3 initClient(AWSCredentialsProvider credProvider, String awsEndpoint) {
		
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

	protected AmazonS3 initS3(String awsEndpoint) {
		AWSCredentialsProvider credProvider = new DefaultAWSCredentialsProviderChain();
		return  initClient(credProvider, awsEndpoint);
	}
	
	public S3Index(String aws_endpoint, String bucket) {
		LoggerContext ctx = initLogger();
		
		lsLogger = ctx.getLogger("lsLogger");
		cmdLogger = ctx.getLogger("cmdLogger");

		s3Client = initS3(aws_endpoint);
		this.bucket=bucket;		
	}
	
	public void listV1() {
        ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucket);
        long c=0;
        String lastKey=null;
        ObjectListing result;
		do {
            result = s3Client.listObjects(req);            
            for (S3ObjectSummary os : result.getObjectSummaries()) {
            	c++;
            	lastKey = os.getKey();
            	if (lastKey.startsWith("arch-") || lastKey.startsWith("0arch-")) {
            		listArchive(os.getKey());
            	} else {
            		log(os.getKey(), null, os.getSize());
            	}
            	if (c%10000==0) {
            		cmdLogger.info("" + c );
            	}
            }
            req.setMarker(lastKey);
        } while (result.isTruncated());		
        System.out.println("Scanned " + c + " entries");		
	}
	
	public void listMetadata(String key) {
		GetObjectRequest req = new GetObjectRequest(bucket, key);		
		GetObjectMetadataRequest mReq = new GetObjectMetadataRequest(bucket, key);
		ObjectMetadata meta = s3Client.getObjectMetadata(mReq);
		
		for (String k : meta.getUserMetadata().keySet()) {
			System.out.println(k + ":" + meta.getUserMetaDataOf(k));			
		}
		
		Object extract= meta.getUserMetadata().get("snowball-auto-extract");
		System.out.println("meta=" + extract);
		System.out.println(extract.equals("true"));
	}
	
	protected void listArchive(String key) {
	
		GetObjectRequest req = new GetObjectRequest(bucket, key);		
		GetObjectMetadataRequest mReq = new GetObjectMetadataRequest(bucket, key);	
		
		File zip=null;
		try {
			Path tmp= Files.createTempFile("s3", ".zip");
			zip = tmp.toFile();					
			//ObjectMetadata meta = s3Client.getObjectMetadata(mReq);
			ObjectMetadata meta = s3Client.getObject(req, zip);
			Object extract= meta.getUserMetadata().get("snowball-auto-extract");
			if ("true".equals(extract)) {
				cmdLogger.info("Archive OK");
				cmdLogger.info("Downloading to " + zip.getAbsolutePath());
				//s3Client.getObject(req, zip);
				introspectZip(key, zip);
			} else {
				cmdLogger.error("Archive KO - no metadata found for key " + key);
			}
		} catch (Exception e) {
			cmdLogger.error("Unable to get Archive", e);
		} finally {
			if (zip!=null) {
				zip.delete();
			}
		}
	}
	
	protected void introspectZipStream(String key, File zip) {
		ZipInputStream zis = null;		
		try {
			zis = new ZipInputStream(new FileInputStream(zip));			
			ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
            	log(ze.getName(), key, ze.getCompressedSize());
            }			
		} catch (Exception e) {
			cmdLogger.error("Unable to read Archive", e);		
		} finally {
			if (zis!=null) {
				try {
					zis.close();
				} catch (IOException e) {
					cmdLogger.error("Unable to close Archive", e);		
				}
			}
		}		
	}

	protected void introspectZip(String key, File zip) {
		
		ZipFile zFile =null;
		try {
			zFile = new ZipFile(zip);		
			Enumeration<? extends ZipEntry> entries = zFile.entries();
			
			while (entries.hasMoreElements()) {				
				ZipEntry ze = entries.nextElement();
            	log(ze.getName(), key, ze.getSize());
            }
			
		} catch (Exception e) {
			cmdLogger.error("Unable to read Archive", e);		
		} finally {
			if (zFile!=null) {
				try {
					zFile.close();
				} catch (IOException e) {
					cmdLogger.error("Unable to close Archive", e);		
				}
			}
		}		
	}

	protected void log(String digest, String archive, long size) {
		StringBuilder sb = new StringBuilder();
		sb.append(digest);
		sb.append(",");
		if (archive!=null) {
			sb.append(archive);	
		}
		sb.append(",");
		sb.append(size);
		
		lsLogger.debug(sb.toString());
	}

}
