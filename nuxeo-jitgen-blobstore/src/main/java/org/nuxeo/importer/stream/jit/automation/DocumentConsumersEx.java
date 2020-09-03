package org.nuxeo.importer.stream.jit.automation;

import static org.nuxeo.elasticsearch.bulk.IndexAction.ACTION_NAME;
import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_DOC_NAME;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.consumer.DocumentConsumerPolicy;
import org.nuxeo.importer.stream.consumer.DocumentConsumerPool;
import org.nuxeo.importer.stream.consumer.DocumentMessageConsumer;
import org.nuxeo.importer.stream.consumer.DocumentMessageConsumerFactory;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.importer.stream.scroll.ImporterCompanionScroller;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.Consumer;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

import net.jodah.failsafe.RetryPolicy;

@Operation(id = DocumentConsumersEx.ID, category = Constants.CAT_SERVICES, label = "Imports document", since = "9.1", description = "Import documents into repository.")
public class DocumentConsumersEx {

	private static final Log log = LogFactory.getLog(DocumentConsumersEx.class);

	public static final String ID = "StreamImporter.runDocumentConsumersEx";

	@Context
	protected OperationContext ctx;

	@Param(name = "nbThreads", required = false)
	protected Integer nbThreads;

	@Param(name = "rootFolder")
	protected String rootFolder;

	@Param(name = "repositoryName", required = false)
	protected String repositoryName;

	@Param(name = "batchSize", required = false)
	protected Integer batchSize = 10;

	@Param(name = "batchThresholdS", required = false)
	protected Integer batchThresholdS = 5;

	@Param(name = "retryMax", required = false)
	protected Integer retryMax = 3;

	@Param(name = "retryDelayS", required = false)
	protected Integer retryDelayS = 2;

	@Param(name = "logName", required = false)
	protected String logName = DEFAULT_LOG_DOC_NAME;

	@Param(name = "blockIndexing", required = false)
	protected Boolean blockIndexing = false;

	@Param(name = "blockAsyncListeners", required = false)
	protected Boolean blockAsyncListeners = false;

	@Param(name = "blockPostCommitListeners", required = false)
	protected Boolean blockPostCommitListeners = false;

	@Param(name = "blockDefaultSyncListeners", required = false)
	protected Boolean blockSyncListeners = false;

	@Param(name = "useBulkMode", required = false)
	protected Boolean useBulkMode = false;

	@Param(name = "waitMessageTimeoutSeconds", required = false)
	protected Integer waitMessageTimeoutSeconds = 20;

	@Param(name = "usePathHack", required = false)
	protected Boolean usePathHack = false;
	
	@Param(name = "useScroller", required = false)
	protected Boolean useScroller = false;
	

	  @Context
	    protected CoreSession session;

	    @Context
	    protected BulkService bulkService;

	@OperationMethod
	public String run() throws OperationException {
		try {
			ParentRefHelper.init();
		} catch (Exception e) {
			throw new OperationException("Unable to unit Cache", e);
		}
		repositoryName = getRepositoryName();
		ConsumerPolicy consumerPolicy = DocumentConsumerPolicy.builder().blockIndexing(blockIndexing)
				.blockAsyncListeners(blockAsyncListeners).blockPostCommitListeners(blockPostCommitListeners)
				.blockDefaultSyncListener(blockSyncListeners).useBulkMode(useBulkMode).name(ID)
				.batchPolicy(BatchPolicy.builder().capacity(batchSize)
						.timeThreshold(Duration.ofSeconds(batchThresholdS)).build())
				.retryPolicy(new RetryPolicy().withMaxRetries(retryMax).withDelay(retryDelayS, TimeUnit.SECONDS))
				.maxThreads(getNbThreads()).waitMessageTimeout(Duration.ofSeconds(waitMessageTimeoutSeconds)).salted()
				.build();
		log.warn(String.format("Import documents from log: %s into: %s/%s, with policy: %s", logName, repositoryName,
				rootFolder, consumerPolicy));
		
		BulkInfo bi=null;
		String scrollerName = null;
		String batchId = null;
		if (useScroller) {
			bi = startBulk();
			scrollerName = bi.scroller;
			batchId = bi.commandId;
		}
		LogManager manager = Framework.getService(StreamService.class).getLogManager();
		Codec<DocumentMessage> codec = StreamImporters.getDocCodec();
		
		try (DocumentConsumerPool<DocumentMessage> consumers = new DocumentConsumerPool<>(logName, manager, codec,
				new DocumentMessageConsumerFactoryEx(repositoryName, rootFolder, scrollerName, usePathHack), consumerPolicy)) {
			String json= ConsumerStatusHelper.aggregateJSON(consumers.start().get(), batchId);
			if (useScroller) {
				ImporterCompanionScroller.kill(scrollerName);
			}
			return json;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Operation interrupted");
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			log.error("Operation fails", e);
			throw new OperationException(e);
		}
	}
	
	protected static class BulkInfo {	
		String commandId;
		String scroller;	
	}
	
	protected BulkInfo startBulk() {
        String username = session.getPrincipal().getName();      
        BulkInfo bi = new BulkInfo();
        boolean syncAlias=false;
        String query = logName;        		
        BulkCommand cmd = new BulkCommand.Builder(ACTION_NAME, query,  username).param(INDEX_UPDATE_ALIAS_PARAM, syncAlias)
                                                                    .repository(getRepositoryName())
                                                                    .useGenericScroller().scroller("DocConsumerScroller")
                                                                    .build();        
        String commandId= bulkService.submit(cmd);
                        
        bi.scroller=query;
        bi.commandId=commandId;
        return bi;       
    }
	
	protected static class DocumentMessageConsumerFactoryEx extends DocumentMessageConsumerFactory {

		final boolean usePathHack;

		final String scrollerId;

		public DocumentMessageConsumerFactoryEx(String repositoryName, String rootPath, String scrollerId,
				boolean usePathHack) {
			super(repositoryName, rootPath);
			this.usePathHack = usePathHack;
			this.scrollerId = scrollerId;
		}

		@Override
		public Consumer<DocumentMessage> createConsumer(String consumerId) {

			if (!usePathHack) {
				return new DocumentMessageConsumerWithScroll(consumerId, repositoryName, rootPath, scrollerId);
			}

			if (rootPath.contains("misc")) {
				return new DocumentMessageConsumerNoPath(consumerId, repositoryName, rootPath, scrollerId);
			} else {
				return new DocumentMessageConsumerCachedPath(consumerId, repositoryName, rootPath, scrollerId);
			}

		}
	}

	protected static class DocumentMessageConsumerWithScroll extends DocumentMessageConsumer {

		protected final String scrollerId;
		protected List<String> ids = new ArrayList<>();

		public DocumentMessageConsumerWithScroll(String consumerId, String repositoryName, String rootPath,
				String scrollerId) {
			super(consumerId, repositoryName, rootPath);
			this.scrollerId = scrollerId;
		}

		protected String getPathForMessage(DocumentMessage message) {
			return rootPath + message.getParentPath();
		}

		protected void beforeCreateDocument(DocumentModel doc, String targetPath) {
			// NOP
		}

		protected void afterCreateDocument(DocumentModel doc) {
			ids.add(doc.getId());
		}

		@Override
		public void commit() {
			super.commit();
			if (scrollerId != null) {
				ImporterCompanionScroller.getInstance(scrollerId).handle(ids);
				ids.clear();
			}
		}

		@Override
		public void accept(DocumentMessage message) {

			String targetPath = getPathForMessage(message);
			DocumentModel doc = session.createDocumentModel(targetPath, message.getName(), message.getType());
			doc.putContextData(CoreSession.SKIP_DESTINATION_CHECK_ON_CREATE, true);
			Blob blob = getBlob(message);
			if (blob != null) {
				doc.setProperty("file", "content", blob);
			}
			Map<String, Serializable> props = message.getProperties();
			if (props != null && !props.isEmpty()) {
				setDocumentProperties(doc, props);
			}

			beforeCreateDocument(doc, targetPath);
			doc = session.createDocument(doc);
			afterCreateDocument(doc);
		}

	}

	protected static class DocumentMessageConsumerNoPath extends DocumentMessageConsumerWithScroll {

		public DocumentMessageConsumerNoPath(String consumerId, String repositoryName, String rootPath,
				String scrollerId) {
			super(consumerId, repositoryName, rootPath, scrollerId);
		}

		protected String getPathForMessage(DocumentMessage message) {
			return rootPath;
		}

	}

	protected static class DocumentMessageConsumerCachedPath extends DocumentMessageConsumerWithScroll {

		public DocumentMessageConsumerCachedPath(String consumerId, String repositoryName, String rootPath,
				String scrollerId) {
			super(consumerId, repositoryName, rootPath, scrollerId);
		}

		protected String getPathForMessage(DocumentMessage message) {
			Path p = new Path(rootPath);
			p = p.append(message.getParentPath());
			return p.toString();
		}

		protected void beforeCreateDocument(DocumentModel doc, String targetPath) {
			// force ParentRef resolution in advance using cache
			ParentRefHelper.getInstance().setParentRef(session, doc, targetPath);
		}

	}

	protected short getNbThreads() {
		if (nbThreads != null) {
			return nbThreads.shortValue();
		}
		return 0;
	}

	protected String getRepositoryName() {
		if (repositoryName != null && !repositoryName.isEmpty()) {
			return repositoryName;
		}
		return ctx.getCoreSession().getRepositoryName();
	}

}
