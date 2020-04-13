/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 */

package org.nuxeo.data.gen.out;

import org.nuxeo.data.gen.pdf.StatementMeta;

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

public class S3Writer extends AbstractBlobWriter implements BlobWriter {

	public static final String NAME = "s3:";

	protected String bucketName;

	protected AWSCredentialsProvider credProvider;

	protected AmazonS3 s3;

	public void initAWSCredentials(String accessKeyId, String secretKey, String sessionToken) {

		if (accessKeyId != null && secretKey != null) {
			if (sessionToken != null) {
				credProvider = new AWSStaticCredentialsProvider(
						new BasicSessionCredentials(accessKeyId, secretKey, sessionToken));
			} else {
				credProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey));
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

		AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder.standard().withCredentials(credProvider)
				.withClientConfiguration(clientConfiguration).withRegion(region);

		s3 = s3Builder.build();

	}

	public S3Writer(String bucketName) {
		this(bucketName, null, null, null);
	}

	public S3Writer(String bucketName, String accessKeyId, String secretKey, String sessionToken) {
		this.bucketName = bucketName;
		initAWSCredentials(accessKeyId, secretKey, sessionToken);
		initClient();
	}

	@Override
	public void write(byte[] data, StatementMeta smeta) throws Exception {

		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		// meta.setContentMD5(digest);

		PutObjectRequest put = new PutObjectRequest(bucketName, smeta.getDigest(), wrap(data), meta);
		PutObjectResult res = s3.putObject(put);
	}

	@Override
	public void flush() {
		// NOP
	}

}
