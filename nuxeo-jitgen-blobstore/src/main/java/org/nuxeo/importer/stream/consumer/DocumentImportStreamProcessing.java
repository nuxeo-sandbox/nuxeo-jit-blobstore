package org.nuxeo.importer.stream.consumer;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.util.Arrays;
import java.util.Map;

import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.elasticsearch.bulk.BulkIndexComputation;
import org.nuxeo.elasticsearch.bulk.IndexAction;
import org.nuxeo.elasticsearch.bulk.IndexRequestComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

public class DocumentImportStreamProcessing implements StreamProcessorTopology {

	public static final String SUFFIX = "-stmt";

	@Override
	public Topology getTopology(Map<String, String> options) {

		int esBulkSize = IndexAction.getOptionAsInteger(options, IndexAction.ES_BULK_SIZE_OPTION,
				IndexAction.ES_BULK_SIZE_DEFAULT);
		int esBulkActions = IndexAction.getOptionAsInteger(options, IndexAction.ES_BULK_ACTION_OPTION,
				IndexAction.ES_BULK_ACTION_DEFAULT);
		int esBulkFlushInterval = IndexAction.getOptionAsInteger(options, IndexAction.BULK_FLUSH_INTERVAL_OPTION,
				IndexAction.BULK_FLUSH_INTERVAL_DEFAULT);

		String defaultRepository = Framework.getService(RepositoryService.class).getRepositoryNames().get(0);
		String repository = options.getOrDefault("repository", defaultRepository);
		String rootPath = options.getOrDefault("rootPath", "/");
		
		return Topology.builder()
				.addComputation(() -> new DocumentMessageConsumerComputation("importDocuments", repository, rootPath),
						Arrays.asList(INPUT_1 + ":import" + SUFFIX, OUTPUT_1 + ":index" + SUFFIX))
				.addComputation(IndexRequestComputation::new, Arrays.asList(INPUT_1 + ":index" + SUFFIX, //
						OUTPUT_1 + ":" + BulkIndexComputation.NAME + SUFFIX))
				.addComputation(() -> new BulkIndexComputation(esBulkSize, esBulkActions, esBulkFlushInterval),
						Arrays.asList(INPUT_1 + ":" + BulkIndexComputation.NAME + SUFFIX, //
								OUTPUT_1 + ":" + STATUS_STREAM + SUFFIX))
				.build();
	}

}
