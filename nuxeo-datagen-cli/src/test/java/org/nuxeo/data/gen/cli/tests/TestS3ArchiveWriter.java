package org.nuxeo.data.gen.cli.tests;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.nuxeo.data.gen.out.S3BulkArchiveWriter;
import org.nuxeo.data.gen.pdf.StatementMeta;

public class TestS3ArchiveWriter {

	
	@Test
	public void testMonoThread() throws Exception {
		
		S3BulkArchiveWriter writer = new S3BulkArchiveWriter("whatever", 1);
		writer.setDebug(true);
		writer.setFlushSize(500);
		
		for (int i = 0; i < 11; i++) {
			String digest = UUID.randomUUID().toString();
			StatementMeta meta = new StatementMeta(digest, "F"+i, 101, null);			
			byte[] data = "*".repeat(101).getBytes(); 
			writer.write(data, meta);
		}
		writer.flush();
		
		List<Path> archives = writer.getArchives();
		assertEquals(3, archives.size());
			
		ZipInputStream zis = new ZipInputStream(new FileInputStream(archives.get(0).toFile()));		
		ZipEntry entry = zis.getNextEntry();
		int count = 0;
		while (entry!=null) {
			count++;
			entry = zis.getNextEntry();
		}
		assertEquals(5, count);;
		zis.close();
		
	}
	

	@Test
	public void testMultiThreads() throws Exception {
		
		ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);
		tpe.prestartAllCoreThreads();

		

		S3BulkArchiveWriter writer = new S3BulkArchiveWriter("whatever", 1);
		writer.setDebug(true);
		writer.setFlushSize(500);
		
		int nbFiles = 10000;
		
		for (int i = 0; i < nbFiles; i++) {			
			tpe.execute(new Runnable() {				
				@Override
				public void run() {
					String digest = UUID.randomUUID().toString();
					StatementMeta meta = new StatementMeta(digest, digest, 101, null);			
					byte[] data = "*".repeat(101).getBytes(); 
					try {
						writer.write(data, meta);
					} catch (Exception e) {
						e.printStackTrace();
					}					
				}
			});			
		}
		
		
		tpe.shutdown();
		boolean finished = false;
		while (!finished) {
			finished = tpe.awaitTermination(5, TimeUnit.SECONDS);
		}		
		writer.flush();
		
		List<Path> archives = writer.getArchives();
		
		int count = 0;
		for (Path file : archives) {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file.toFile()));		
			ZipEntry entry = zis.getNextEntry();
			while (entry!=null) {
				count++;
				entry = zis.getNextEntry();
			}
			zis.close();
			
		}
		assertEquals(nbFiles, count);;
		
	}

}
