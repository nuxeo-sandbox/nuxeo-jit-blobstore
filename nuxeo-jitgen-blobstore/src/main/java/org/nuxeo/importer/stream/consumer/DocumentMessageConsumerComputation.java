package org.nuxeo.importer.stream.consumer;

import static java.lang.Math.min;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.computation.AbstractBatchComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

public class DocumentMessageConsumerComputation extends AbstractBatchComputation {

	protected final List<String> documentIds;
	
	protected final Consumer consumer;
	
	public DocumentMessageConsumerComputation(String name, String repositoryName, String rootPath) {
		super(name, 1, 1);
		documentIds = new ArrayList<String>();
		consumer = new Consumer(name, repositoryName, rootPath);
	}

	protected class Consumer extends DocumentMessageConsumer {

		public Consumer(String consumerId, String repositoryName, String rootPath) {
			super(consumerId, repositoryName, rootPath);
		}
				
		// need to override the default method 
		// because we need to get the uid from the created doc!
	    public DocumentModel importDoc(DocumentMessage message) {
		        DocumentModel doc = session.createDocumentModel(rootPath + message.getParentPath(), message.getName(),
		                message.getType());
		        doc.putContextData(CoreSession.SKIP_DESTINATION_CHECK_ON_CREATE, true);
		        Blob blob = getBlob(message);
		        if (blob != null) {
		            doc.setProperty("file", "content", blob);
		        }
		        Map<String, Serializable> props = message.getProperties();
		        if (props != null && !props.isEmpty()) {
		            setDocumentProperties(doc, props);
		        }
		        return session.createDocument(doc);
	    }	    
	}
	
	@Override
	public void batchFailure(ComputationContext ctx, String inputStreamName, List<Record> records) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void batchProcess(ComputationContext ctx, String inputStreamName, List<Record> records) {

		Codec<DocumentMessage> codec = new SerializableCodec<DocumentMessage>();		
		for (Record record: records) {			
			DocumentMessage msg = codec.decode(record.getData());			
			DocumentModel doc = consumer.importDoc(msg);
			documentIds.add(doc.getId());
		}		
		produceBucket(ctx, "a", "c", documentIds.size(), 1);				
	}

    protected void produceBucket(ComputationContext context, String action, String commandId, int bucketSize,
            long bucketNumber) {
        List<String> ids = documentIds.subList(0, min(bucketSize, documentIds.size()));
        BulkBucket bucket = new BulkBucket(commandId, ids);
        String key = commandId + ":" + Long.toString(bucketNumber);
        Record record = Record.of(key, BulkCodecs.getBucketCodec().encode(bucket));
        context.produceRecord(action, record);
        ids.clear(); // this clear the documentIds part that has been sent
    }

}
