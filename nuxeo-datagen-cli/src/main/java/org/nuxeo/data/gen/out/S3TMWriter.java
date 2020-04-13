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

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class S3TMWriter extends S3Writer {

	public static final String NAME = "s3tm:";

	protected TransferManager tm;

	public S3TMWriter(String bucketName) {
		this(bucketName, null, null, null);
	}

	public S3TMWriter(String bucketName, String accessKeyId, String secretKey, String sessionToken) {
		super(bucketName, accessKeyId, secretKey, sessionToken);

		tm = TransferManagerBuilder.standard().withMultipartUploadThreshold(1024L * 1024).withS3Client(s3)
//                 .withExecutorFactory(executorFactory)
				.build();
	}

	@Override
	public void write(byte[] data, StatementMeta smeta) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		// meta.setContentMD5(digest);

		Upload upload = tm.upload(bucketName, smeta.getDigest(), wrap(data), meta);
		upload.waitForCompletion();

	}

	@Override
	public void flush() {
		// NOP
	}

}
