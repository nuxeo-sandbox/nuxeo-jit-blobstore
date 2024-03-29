package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.jit.es.StatementESDocumentWriter;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.io.JsonESDocumentWriter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.importer.stream.automation.DocumentConsumers;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.jit.automation.CustomerFolderProducers;
import org.nuxeo.importer.stream.jit.automation.CustomerProducers;
import org.nuxeo.importer.stream.jit.automation.StatementFolderProducers;
import org.nuxeo.importer.stream.jit.automation.StatementProducers;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@RunWith(FeaturesRunner.class)
@Features({AutomationFeature.class, RepositoryElasticSearchFeature.class})
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-blobdispatcher-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/import-node-config.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-storage-repo-mem-contrib.xml")
@PartialDeploy(bundle = "studio.extensions.nuxeo-benchmark-10b-2020", extensions = { TargetExtensions.ContentModel.class })
public class TestStreamStatementImporter {

	@Inject
	protected CoreSession session;

	@Inject
	protected AutomationService automationService;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected BulkService bulk;

    @Inject
    ElasticSearchService ess;

    @Inject
    ElasticSearchIndexing esi;

    @Inject
    ElasticSearchAdmin esa;

	@Test
	public void canImportStatements() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			DocumentModel root = session.createDocumentModel("/", "root", "Folder");
			root = session.createDocument(root);
			TransactionHelper.commitOrRollbackTransaction();
			TransactionHelper.startTransaction();

			// *************************
			// create Folder messages
			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");

			automationService.run(ctx, StatementFolderProducers.ID, params);

			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("nbThreads", 1);
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = session
					.query("select * from Folder where ecm:path STARTSWITH '/root' order by ecm:path");
			dump(docs);
			assertEquals(48 + 4, docs.size());

			// *************************
			// create Stmt messages
			long nbDocs = 8 * 15;
			params = new HashMap<>();
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");
			automationService.run(ctx, StatementProducers.ID, params);

			// *************************
			// consume Stmt messages
			params = new HashMap<>();
			params.put("nbThreads", 1);
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			automationService.run(ctx, DocumentConsumers.ID, params);
						
			docs = session.query("select * from Statement order by ecm:path");
			//dump(docs);
			
			
			JsonESDocumentWriter stdWriter = new StatementESDocumentWriter();
			JsonESDocumentWriter limitedWriter = new StatementESDocumentWriter() {			
				@Override
				protected boolean limitedIndexing(DocumentModel doc) {
					return true;
				}
			};
			
			StringWriter writer = new StringWriter();
			JsonGenerator jg = new JsonFactory().createGenerator(writer); 
			jg.useDefaultPrettyPrinter();
			
			System.out.println("full ES Json");
			stdWriter.writeESDocument(jg, docs.get(0), null, null);
			System.out.print(writer.toString());			
			writer.close();
			
			writer = new StringWriter();
			jg = new JsonFactory().createGenerator(writer); 
			jg.useDefaultPrettyPrinter();
			
			System.out.println("reduced ES Json");
			limitedWriter.writeESDocument(jg, docs.get(0), null, null);
			System.out.print(writer.toString());
			writer.close();
			
			assertEquals(nbDocs, docs.size());
			
			esi.indexNonRecursive(new IndexingCommand(docs.get(0), IndexingCommand.Type.INSERT, true, false));
			
			waitForCompletion();
			StatementESDocumentWriter esWriter = new StatementESDocumentWriter();
			for (DocumentModel doc : docs) {
				System.out.println(esWriter.getFullText(doc));
			}
		}
	}

	 public void waitForCompletion() throws Exception {
	        bulk.await(Duration.ofSeconds(20));
	        workManager.awaitCompletion(20, TimeUnit.SECONDS);
	        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
	        esa.refresh();
	    }

	
	@Test
	public void canImportCustomers() throws Exception {

		// lazy way to create a Blob
		String blobTextContent = "I am a fake ID card!";
		DocumentModel file = session.createDocumentModel("/", "someFilet", "File");
		Blob idblob = new StringBlob(blobTextContent);
		idblob.setFilename("whatever.jpg");
		//file.setPropertyValue("file:content", (Serializable)idblob);
		//file = session.createDocument(file);
	
		
		BlobManager bm = Framework.getService(BlobManager.class);
		Map<String, BlobProvider> providers = bm.getBlobProviders();
		BlobProvider snowball = bm.getBlobProvider("snowball");
		assertNotNull(snowball);
		
		String blobDigest = snowball.writeBlob(idblob);
		snowball.getBinaryManager().getBinary(idblob);
		
		//snowball.
		//Blobidblob = (Blob) file.getPropertyValue("file:content");
		//String blobDigest = idblob.getDigest();
		
		
		
		System.out.println(blobDigest);		
		
		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			DocumentModel root = session.createDocumentModel("/", "root", "Folder");
			root = session.createDocument(root);
			TransactionHelper.commitOrRollbackTransaction();
			TransactionHelper.startTransaction();

			// *************************
			// create Folder messages
			params.put("logConfig", "chronicle");

			automationService.run(ctx, CustomerFolderProducers.ID, params);

			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = session
					.query("select * from Domain where ecm:path STARTSWITH '/root' order by ecm:path");
			//dump(docs);
			assertEquals(USStateHelper.STATES.length, docs.size());

			
			// *************************
			// create Customers messages
			params.put("logConfig", "chronicle");
			params.put("bufferSize", 5);
			//params.put("nbThreads", 1);	
			
			InputStream csv = StatementsBlobGenerator.class.getResourceAsStream("/id-cards.csv");
			String csvContent = new String(IOUtils.toByteArray(csv));			
			csvContent = csvContent.replace("<DIGEST>", blobDigest);			
			Blob blob = new StringBlob(csvContent);
								
			ctx.setInput(blob);
			automationService.run(ctx, CustomerProducers.ID, params);

			// *************************
			// consume Customers messages
			params = new HashMap<>();
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			//params.put("nbThreads", 1);	
			automationService.run(ctx, DocumentConsumers.ID, params);

			docs = session
					.query("select * from Document where ecm:primaryType IN ('Customer', 'IDCard') order by ecm:path");

			//dump(docs);
			assertEquals(200, docs.size());
			
			docs = session
					.query("select * from IDCard order by ecm:path");

			Blob importedBlob = (Blob) docs.get(0).getPropertyValue("file:content");
			assertNotNull(importedBlob);
			assertEquals("image/jpeg", importedBlob.getMimeType());
			assertTrue(importedBlob.getFilename().startsWith("IDCard"));

			// got you !			
			assertEquals(blobTextContent, importedBlob.getString());				
		}
	}
	
	
	@Test
	public void canImportStatementsWithStates() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			DocumentModel root = session.createDocumentModel("/", "root", "Folder");
			root = session.createDocument(root);
			TransactionHelper.commitOrRollbackTransaction();
			TransactionHelper.startTransaction();

			// *************************
			// create Folder messages
			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");
			params.put("withStates", true);

			automationService.run(ctx, StatementFolderProducers.ID, params);

			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("nbThreads", 1);
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = session
					.query("select * from  Folder where ecm:path STARTSWITH '/root' order by ecm:path");
			//dump(docs);
			assertEquals((48+4+1)*USStateHelper.STATES.length, docs.size());

			// *************************
			// create File messages
			long nbDocs = 8 * 52 * 15;
			params = new HashMap<>();
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 48);
			params.put("withStates", true);
			params.put("logConfig", "chronicle");
			automationService.run(ctx, StatementProducers.ID, params);

			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("nbThreads", 1);
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			automationService.run(ctx, DocumentConsumers.ID, params);

			docs = session.query("select * from Statement order by ecm:path");
			//dump(docs);
			assertEquals(nbDocs, docs.size());
			
		}
	}
	
	protected void dump(DocumentModelList docs) {
		for (DocumentModel doc : docs) {
			System.out.println(doc.getType() + ": " + doc.getPathAsString() + " -- " + doc.getTitle());
		}
	}

}
