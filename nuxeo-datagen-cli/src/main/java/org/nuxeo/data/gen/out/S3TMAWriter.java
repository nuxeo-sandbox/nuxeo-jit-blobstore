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

import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.data.gen.pdf.StatementMeta;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Upload;

public class S3TMAWriter extends S3TMWriter {

	public static final String NAME = "s3tma:";

	protected AtomicInteger pendingUploads = new AtomicInteger();

	public S3TMAWriter(String bucketName) {
		super(bucketName, null, null, null, null);
	}

	public S3TMAWriter(String bucketName, String accessKeyId, String secretKey, String sessionToken, String awsEndPoint) {
		super(bucketName, accessKeyId, secretKey, sessionToken, awsEndPoint);
	}

	@Override
	public void write(byte[] data, StatementMeta smeta) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		// meta.setContentMD5(digest);

		Upload upload = tm.upload(bucketName, smeta.getDigest(), wrap(data), meta);
		pendingUploads.incrementAndGet();
		upload.addProgressListener(new ProgressListener() {

			@Override
			public void progressChanged(ProgressEvent evt) {
				if ((evt.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT)) {
					pendingUploads.decrementAndGet();
				} else if ((evt.getEventType() == ProgressEventType.TRANSFER_CANCELED_EVENT)
						|| (evt.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT)) {
					System.out.println("Transfer Error!");
					pendingUploads.decrementAndGet();
				} else {
					// System.out.println(evt.getEventType().);
				}
			}
		});

	}

	@Override
	public void flush() {
		while (pendingUploads.get() > 0) {
			try {
				System.out.println("Waiting for " + pendingUploads.get() + " uploads to complete");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
