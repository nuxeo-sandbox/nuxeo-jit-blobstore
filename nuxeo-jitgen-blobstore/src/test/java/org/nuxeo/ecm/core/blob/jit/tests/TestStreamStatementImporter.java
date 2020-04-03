package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.importer.stream.automation.DocumentConsumers;
import org.nuxeo.importer.stream.jit.automation.StatementFolderProducers;
import org.nuxeo.importer.stream.jit.automation.StatementProducers;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-blobdispatcher-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
@PartialDeploy(bundle = "studio.extensions.nuxeo-benchmark-10b-2020", extensions = { TargetExtensions.ContentModel.class })
public class TestStreamStatementImporter {

	@Inject
	protected CoreSession session;

	@Inject
	protected AutomationService automationService;

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
			// dump(docs);
			assertEquals(48 + 4, docs.size());

			// *************************
			// create File messages
			long nbDocs = 8 * 15;
			params = new HashMap<>();
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");
			automationService.run(ctx, StatementProducers.ID, params);

			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("nbThreads", 1);
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			automationService.run(ctx, DocumentConsumers.ID, params);

			docs = session.query("select * from File order by ecm:path");
			dump(docs);
			assertEquals(nbDocs, docs.size());

		}
	}

	protected void dump(DocumentModelList docs) {
		for (DocumentModel doc : docs) {
			System.out.println(doc.getPathAsString());
			System.out.println(doc.getPathAsString());
		}
	}

}
