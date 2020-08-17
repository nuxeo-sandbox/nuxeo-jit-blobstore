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

package org.nuxeo.importer.stream.jit.automation;

import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.jit.StatementDocumentMessageProducerFactory;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.AvroBinaryCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

@Operation(id = StatementProducers.ID, category = Constants.CAT_SERVICES, label = "Produces Bank Statements", since = "11.1", description = "")
public class StatementProducers {
    private static final Log log = LogFactory.getLog(StatementProducers.class);

    public static final String ID = "StreamImporter.runStatementProducers";

    @Context
    protected OperationContext context;

    @Param(name = "nbDocuments")
    protected Long nbDocuments;

    @Param(name = "nbMonths")
    protected Integer nbMonths = 12*10;

    @Param(name = "monthOffset" , required = false)
    protected Integer monthOffset = 0;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads = 8;

    @Param(name = "logName", required = false)
    protected String logName = StreamImporters.DEFAULT_LOG_DOC_NAME;

    @Param(name = "logSize", required = false)
    protected Integer logSize;

    @Param(name = "logConfig", required = false)
    protected String logConfig = StreamImporters.DEFAULT_LOG_CONFIG;

    @Param(name = "seed", required = false)
    protected Long seed = SequenceGenerator.DEFAULT_ACCOUNT_SEED;

    @Param(name = "skip", required = false)
    protected Long skip = 0L;

    @Param(name = "batchTag", required = false)
    protected String batchTag = null;

    @Param(name = "useRecords", required = false)
    protected boolean useRecords = false;
        
	@Param(name = "withStates", required = false)
	protected boolean withStates = false;
	
	@Param(name = "storeInCustomerFolder", required = false)
	protected boolean storeInCustomerFolder = false;		

    protected void checkAccess() {
        NuxeoPrincipal principal = context.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }

    @OperationMethod
    public void run() throws OperationException {
        
    	checkAccess();
        
        LogManager manager = Framework.getService(StreamService.class).getLogManager(logConfig);
        manager.createIfNotExists(logName, getLogSize());

        StatementDocumentMessageProducerFactory factory;
        
        long docPerThreads = nbDocuments/nbThreads;
        if (nbDocuments%nbThreads!=0) {
        	docPerThreads++;
        }

        factory = new StatementDocumentMessageProducerFactory(seed, skip, docPerThreads, nbMonths, monthOffset, batchTag, useRecords, withStates, storeInCustomerFolder);

        ProducerPool producers=null;
        try {        	
            if (!useRecords){
                Codec<DocumentMessage> codec = StreamImporters.getDocCodec();
            	producers = new ProducerPool<DocumentMessage>(logName, manager, codec, factory, nbThreads.shortValue());
            } else {
            	Codec<Message> codec = new AvroBinaryCodec<>(Message.class);
            	producers = new ProducerPool<Message>(logName, manager, codec, factory, nbThreads.shortValue());
            }        		
            producers.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Operation interrupted");
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("Operation fails", e);
            throw new OperationException(e);
        } finally {
        	producers.close();
		}
    }

    protected int getLogSize() {
        if (logSize != null && logSize > 0) {
            return logSize;
        }
        return nbThreads;
    }

}
