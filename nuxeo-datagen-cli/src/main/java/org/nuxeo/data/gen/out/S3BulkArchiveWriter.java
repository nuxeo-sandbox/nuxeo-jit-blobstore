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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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

	public static final String SB_AUTO_EXTRACT_PROP = "snowball-auto-extract";

	protected final ThreadPoolExecutor tpe;

	protected Long currentArchiveSize = 0L;

	protected ByteArrayOutputStream zipBuffer;

	protected ZipOutputStream zipArchive;

	protected static final long DEFAULT_FLUSH_SIZE = 1024 * 1024;

	protected boolean debug = false;

	protected List<Path> debugArchives = new ArrayList<Path>();

	protected long flushSize = DEFAULT_FLUSH_SIZE;

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public long getFlushSize() {
		return flushSize;
	}

	public void setFlushSize(long flushSize) {
		this.flushSize = flushSize;
	}

	public S3BulkArchiveWriter(String bucketName, int nbThreads) {
		this(bucketName, nbThreads, null, null, null, null);
	}

	public S3BulkArchiveWriter(String bucketName, int nbThreads, String accessKeyId, String secretKey,
			String sessionToken, String awsEndPoint) {
		super(bucketName, accessKeyId, secretKey, sessionToken, awsEndPoint);
		tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		tpe.prestartAllCoreThreads();
		initArchive();
	}

	@Override
	public void write(byte[] data, StatementMeta smeta) throws Exception {
		addToBuffer(data, smeta);
		if (currentArchiveSize > getFlushSize()) {
			flushArchive();
		}
	}

	protected void initArchive() {
		zipBuffer = new ByteArrayOutputStream();
		zipArchive = new ZipOutputStream(zipBuffer);
		zipArchive.setLevel(0);
		currentArchiveSize = 0L;
	}

	protected synchronized void flushArchive() throws Exception {
		zipArchive.close();
		byte[] data = zipBuffer.toByteArray();

		if (data.length > 100) {
			tpe.execute(new Runnable() {
				@Override
				public void run() {

					try {
						if (debug) {
							flushArchiveToFS(data);
						} else {
							flushArchiveToS3(data);
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			});
		}
		initArchive();

	}

	protected void flushArchiveToS3(byte[] data) throws Exception {
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(data.length);
		// snowball-auto-extract=true
		meta.addUserMetadata(SB_AUTO_EXTRACT_PROP, "true");
		try {
			PutObjectRequest put = new PutObjectRequest(bucketName, "arch-" + UUID.randomUUID().toString(), wrap(data),
					meta);
			PutObjectResult res = s3.putObject(put);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void flushArchiveToFS(byte[] data) throws Exception {

		Path tmp = Files.createTempFile("arch-", ".zip");

		try {
			Files.copy(new ByteArrayInputStream(data), tmp, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		debugArchives.add(tmp);
	}

	protected synchronized void addToBuffer(byte[] data, StatementMeta smeta) throws Exception {
		ZipEntry entry = new ZipEntry(smeta.getDigest());
		zipArchive.putNextEntry(entry);
		zipArchive.write(data);
		zipArchive.closeEntry();
		currentArchiveSize += data.length;
	}

	public List<Path> getArchives() {
		return debugArchives;
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
