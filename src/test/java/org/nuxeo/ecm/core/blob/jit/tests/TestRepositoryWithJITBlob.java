package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.jit.JITBlobProvider;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-blobdispatcher-contrib.xml")
public class TestRepositoryWithJITBlob {

	@Inject
	protected CoreSession session;
	
    @Inject
    protected BlobManager blobManager;

	@Inject
	protected InMemoryBlobGenerator imbg;

	//@Test
	public void testCreateDocWithJITBlob() throws Exception {
				
		BlobProvider bp = blobManager.getBlobProvider("test");
		assertNotNull(bp);		
		// CoreSession has been initialized before the BlobDispatcher config ..
		assertEquals(JITBlobProvider.class, bp.getClass());

		DocumentModel doc = session.createDocumentModel("File");	
		doc.setPathInfo("/", "file");
		doc.setPropertyValue("dc:title", "Yo");
			
		
		String key = imbg.computeKey(1L, 1L, 1);
		
		BlobInfo bi = new BlobInfo();
		bi.key="test:"+key;
 
		Blob blob = new SimpleManagedBlob(bi);
		doc.setPropertyValue("file:content", (Serializable)blob); 
		doc.setPropertyValue("dc:format", "jit");
		
		doc = session.createDocument(doc);
		
		doc = session.getDocument(doc.getRef());
		
		blob = (Blob)doc.getPropertyValue("file:conten");		
		
		InputStream pdf = blob.getStream();
		
		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(pdf));
	    
		assertTrue(txt.contains("Account"));
		assertTrue(txt.contains("Balance"));
		
	}
	
	
}
