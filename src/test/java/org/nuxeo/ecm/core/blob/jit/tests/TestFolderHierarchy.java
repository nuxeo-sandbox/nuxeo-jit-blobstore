package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.jit.StatementFolderMessageProducer;
import org.nuxeo.importer.stream.jit.automation.StatementFolderProducers;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
public class TestFolderHierarchy {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void canCreateTimeHierarchy() throws Exception{

    	try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Serializable> params = new HashMap<>();

            params.put("nbMonths", 48);
            params.put("logConfig", "chronicle");

            automationService.run(ctx,StatementFolderProducers.ID, params);
                                    
    		LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");
    		
            LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);            

            int count=0;
            LogRecord<DocumentMessage> record=null;
            do {
            	record = tailer.read(Duration.ofSeconds(1));
            	if (record!=null) {
            		DocumentMessage docMessage = record.message();
            		assertEquals("Folder",docMessage.getType());
            		assertTrue(((String)docMessage.getProperties().get("dc:description")).startsWith(StatementFolderMessageProducer.FOLDER_DESC_PREFIX));            		
            		count++;
            	}            		
            } while (record!=null);
            
            assertEquals(48+4, count);
            tailer.close();
        }
    }

	
}
