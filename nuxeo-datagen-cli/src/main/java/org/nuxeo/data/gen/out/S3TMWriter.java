package org.nuxeo.data.gen.out;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class S3TMWriter extends S3Writer {

	public static final String NAME= "s3tm:";

	protected TransferManager tm;
	
	
	public S3TMWriter(String bucketName) {
		this(bucketName, null,null,null);
	}

	public S3TMWriter (String bucketName, String accessKeyId, String secretKey,
            String sessionToken) {
		super(bucketName,accessKeyId, secretKey, sessionToken);
		
		tm = TransferManagerBuilder.standard().withMultipartUploadThreshold(1024L*1024)				
                 .withS3Client(s3)
//                 .withExecutorFactory(executorFactory)
                 .build();			
	}
	
	
	
	@Override
	public void write(byte[] data, String digest) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		//meta.setContentMD5(digest);

		Upload upload = tm.upload(bucketName, digest,wrap(data), meta);
		upload.waitForCompletion();
		
	}

	@Override
	public void flush() {
		// NOP
	}


	
}
