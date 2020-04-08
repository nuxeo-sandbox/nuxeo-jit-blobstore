package org.nuxeo.data.gen.out;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Upload;

public class S3TMAWriter extends S3TMWriter {

	public static final String NAME= "s3tma:";

	protected AtomicInteger pendingUploads = new AtomicInteger();
	
	public S3TMAWriter(String bucketName) {
		super(bucketName, null,null,null);
	}
	
	public S3TMAWriter(String bucketName, String accessKeyId, String secretKey, String sessionToken) {
		super(bucketName, accessKeyId, secretKey, sessionToken);
	}

	@Override
	public void write(byte[] data, String digest) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		//meta.setContentMD5(digest);

		Upload upload = tm.upload(bucketName, digest,wrap(data), meta);
		pendingUploads.incrementAndGet();
		upload.addProgressListener(new ProgressListener() {
			
			@Override
			public void progressChanged(ProgressEvent evt) {
				if ((evt.getEventType()==ProgressEventType.TRANSFER_COMPLETED_EVENT)) {
					pendingUploads.decrementAndGet();
				} else if (	(evt.getEventType()==ProgressEventType.TRANSFER_CANCELED_EVENT) ||
							(evt.getEventType()==ProgressEventType.TRANSFER_FAILED_EVENT)) {
					System.out.println("Transfer Error!");
					pendingUploads.decrementAndGet();
				} else {
					//System.out.println(evt.getEventType().);
				}
			}
		});
			
	}

	@Override
	public void flush() {
		while (pendingUploads.get()>0) {
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
