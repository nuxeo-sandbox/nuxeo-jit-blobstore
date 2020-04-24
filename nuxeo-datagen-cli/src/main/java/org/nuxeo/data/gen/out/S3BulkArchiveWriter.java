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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.nuxeo.data.gen.pdf.StatementMeta;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

// https://docs.aws.amazon.com/snowball/latest/developer-guide/batching-small-files.html

public class S3BulkArchiveWriter extends S3Writer {

	public static final String NAME = "s3a:";

	protected final ThreadPoolExecutor tpe;
	
	protected Long currentArchiveSize = 0L;
	
	protected ByteArrayOutputStream zipBuffer;
	protected ZipOutputStream zipArchive;
	protected static final long FLUSH_SIZE = 1024*1024;

	protected boolean debug=false;
	
	public S3BulkArchiveWriter(String bucketName, int nbThreads) {
		this(bucketName, nbThreads, null, null, null, null);
	}

	public S3BulkArchiveWriter(String bucketName,  int nbThreads, String accessKeyId, String secretKey, String sessionToken, String awsEndPoint) {
		super(bucketName, accessKeyId, secretKey, sessionToken, awsEndPoint);
		tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		tpe.prestartAllCoreThreads();
		initArchive();

	}
	
	@Override
	public synchronized void write(byte[] data, StatementMeta smeta) throws Exception {
		addToBuffer(data, smeta);	
		if (currentArchiveSize> FLUSH_SIZE) {
			flushArchive();
		}
	}

	protected void initArchive() {
		zipBuffer = new ByteArrayOutputStream();
		zipArchive = new ZipOutputStream(zipBuffer);
		zipArchive.setLevel(0);
		currentArchiveSize=0L;
	}
	
	protected synchronized void flushArchive() throws Exception {		
		if (debug) {
			flushArchiveDebug();
		} else {
			flushArchiveToS3();
		}		
	}
	
	protected synchronized void flushArchiveToS3() throws Exception {
		zipArchive.close();		
		byte[] data = zipBuffer.toByteArray();
		
		tpe.execute(new Runnable() {			
			@Override
			public void run() {
				ObjectMetadata meta = new ObjectMetadata();
				meta.setContentLength(data.length);
				// snowball-auto-extract=true
				meta.addUserMetadata("snowball-auto-extract", "true");				
				try {
					PutObjectRequest put = new PutObjectRequest(bucketName, "arch-" + UUID.randomUUID().toString(), wrap(data), meta);
					PutObjectResult res = s3.putObject(put);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		initArchive();		
		
	}

	protected synchronized void flushArchiveDebug() throws Exception {
		zipArchive.close();		
		byte[] data = zipBuffer.toByteArray();
		
		tpe.execute(new Runnable() {			
			@Override
			public void run() {
				
				String fname = "arch-" + UUID.randomUUID();
				
				File tmp = new File(fname);
				try {
					Files.copy(new ByteArrayInputStream(data), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				System.out.println("Saveed as " + tmp.getAbsolutePath());
			}
		});
		
		initArchive();		
		
	}

	protected synchronized void addToBuffer(byte[] data, StatementMeta smeta) throws Exception {
		  ZipEntry entry = new ZipEntry(smeta.getDigest());			  
		  zipArchive.putNextEntry(entry);
		  zipArchive.write(data);
		  zipArchive.closeEntry();
		  currentArchiveSize+=data.length;	
		  
	}
	
	@Override
	public void flush() {		
		try {
			flushArchive();
			tpe.shutdown();
			boolean finished = false;
			while (!finished) {
				finished = tpe.awaitTermination(5, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
