package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
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
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.importer.stream.automation.DocumentConsumers;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.jit.automation.CustomerFolderProducers;
import org.nuxeo.importer.stream.jit.automation.CustomerProducers;
import org.nuxeo.importer.stream.jit.automation.StatementFolderProducers;
import org.nuxeo.importer.stream.jit.automation.StatementProducers;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({AutomationFeature.class, RepositoryElasticSearchFeature.class})
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-blobdispatcher-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/import-pipeline-stream-contrib.xml")
@PartialDeploy(bundle = "studio.extensions.nuxeo-benchmark-10b-2020", extensions = { TargetExtensions.ContentModel.class })
public class TestStreamStatementImportPipeline {

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
    
//    @Inject
//    LogManager logManager;
    
//    @Inject 
//    StreamManager streamManager;
    
	@Test
	public void canImportStatements() throws Exception {

        if (true) {
        	// unfinished code
        	return;
        }

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
			// dump(docs);
			assertEquals(48 + 4, docs.size());

			// *************************
			// create File messages
			long nbDocs = 8 * 15;
			params = new HashMap<>();
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");
			params.put("logName", "import-stmt");
			params.put("useRecords", true);
			automationService.run(ctx, StatementProducers.ID, params);


			
			//streamManager.
			
			//StreamManager streamManager = streamManager.getStreamManager("default");

			
			docs = session.query("select * from Statement order by ecm:path");
			int retry=100;
			while (docs.size()==0 && retry >0) {
				Thread.sleep(500);
				retry--;
			}
			dump(docs);
			assertEquals(nbDocs, docs.size());
			
			esi.indexNonRecursive(new IndexingCommand(docs.get(0), IndexingCommand.Type.INSERT, true, false));
			
			StatementESDocumentWriter esWriter = new StatementESDocumentWriter();
			for (DocumentModel doc : docs) {
				System.out.println(esWriter.getFullText(doc));
			}
		}
	}

	

	protected void dump(DocumentModelList docs) {
		for (DocumentModel doc : docs) {
			System.out.println(doc.getType() + ": " + doc.getPathAsString() + " -- " + doc.getTitle());
		}
	}

}
