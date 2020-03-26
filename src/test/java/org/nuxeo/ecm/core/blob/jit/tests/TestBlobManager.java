package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.inject.Inject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStore.OptionalOrUnknown;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.CachingBlobStore;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.jit.JITBlobProvider;
import org.nuxeo.ecm.core.blob.jit.JITBlobStore;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-jit-blob-provider.xml")
public class TestBlobManager {

	@Inject
	protected InMemoryBlobGenerator imbg;
	
    @Inject
    protected BlobManager blobManager;

	@Test
	public void canAccessBlobStoreWithCache() throws Exception {
		
		// check BlobProvider
		BlobProvider bp = blobManager.getBlobProvider("test");
		assertNotNull(bp);
		assertEquals(JITBlobProvider.class, bp.getClass());
		
		validateBlobStore(bp, true);	
		
		validateManagedBlob(bp);
	}

	@Test
	public void canAccessBlobStoreNoCache() throws Exception {
		
		// check BlobProvider
		BlobProvider bp = blobManager.getBlobProvider("test2");
		assertNotNull(bp);
		assertEquals(JITBlobProvider.class, bp.getClass());
		
		validateBlobStore(bp, true);				
	}

	protected void validateManagedBlob(BlobProvider bp) throws Exception {
		String key = imbg.computeKey(1L, 1L, 1);
		
		BlobInfo bi = new BlobInfo();
		bi.key="test:"+key;
		
		ManagedBlob blob = new SimpleManagedBlob(bi);				
		
		InputStream pdf = bp.getStream(blob);
		
		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(pdf));
	    
		assertTrue(txt.contains("Account"));
		assertTrue(txt.contains("Balance"));			
	}
	
	protected void validateBlobStore(BlobProvider bp, boolean cache) throws Exception {

		String key = imbg.computeKey(1L, 1L, 1);
		Map<String, String> meta = imbg.getMetaDataKey(key);
		
		BlobStore bs;
		if (cache) {
			// get the caching BlobStore
			BlobStore cbs = ((BlobStoreBlobProvider) bp).store;
			assertNotNull(cbs);
			assertEquals(CachingBlobStore.class, cbs.getClass());
		
			// get the actual JITBlobStore
			bs = cbs.unwrap();
			assertEquals(JITBlobStore.class, bs.getClass());			
		} else {
			bs = ((BlobStoreBlobProvider) bp).store;
			assertNotNull(bs);
			assertEquals(JITBlobStore.class, bs.getClass());
		}
		
		// check access
		
        Path tmp = Files.createTempFile("tmp_", ".tmp");		
		assertTrue(bs.readBlob(key, tmp));
		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(tmp.toFile()));
		
		//String hexd1= ITextNXBankStatementGenerator.toHexString(Files.readAllBytes(tmp));
	    
		assertTrue(txt.contains("Account"));
		assertTrue(txt.contains("Balance"));
		
		OptionalOrUnknown<Path> op =bs.getFile(key);
		Path p = op.get();
		assertNotNull(p);
		String txt2 = stripper.getText(PDDocument.load(p.toFile()));		
		assertEquals(txt, txt2);
		
		// with cache this should work ???
		// 
		//String hexd2= ITextNXBankStatementGenerator.toHexString(Files.readAllBytes(p));	   
		//assertEquals(hexd1, hexd2);		
		
		OptionalOrUnknown<InputStream> os = bs.getStream(key);
		InputStream s = os.get();
		assertNotNull(s);
		String txt3 = stripper.getText(PDDocument.load(s));		
		assertEquals(txt, txt3);

	}
	
}
