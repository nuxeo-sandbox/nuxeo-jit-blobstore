package org.nuxeo.importer.stream.producer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.lib.stream.pattern.producer.ProducerStatus;

public class MultiRepositoriesProducerPool<M extends DocumentMessage> extends ProducerPool<M> {

	protected final boolean split;
	
	public MultiRepositoriesProducerPool(String logName, LogManager manager, Codec<M> codec, ProducerFactory<M> factory,
			short nbThreads, boolean split) {
		super(logName, manager, codec, factory, nbThreads);
		this.split=split;
	}

	public static Name getLogName(String logNamePrefix, String region) {
		return Name.ofUrn(logNamePrefix+"-" + region);
	}
	
	@Override
	protected Callable<ProducerStatus> getCallable(int i) {
		
		if (split) {
			Map<String, LogAppender<M>> appenders = new HashMap<>();		
			appenders.put(USStateHelper.EST, manager.getAppender(getLogName(logName,USStateHelper.EST), codec));
			appenders.put(USStateHelper.WEST, manager.getAppender(getLogName(logName,USStateHelper.WEST), codec));				
			return new MultiRepositoriesProducer<>(factory,appenders , i);		
		} else {
			return super.getCallable(i);
		}
		
	}

}
