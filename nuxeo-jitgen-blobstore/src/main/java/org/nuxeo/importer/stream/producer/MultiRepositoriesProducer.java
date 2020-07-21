package org.nuxeo.importer.stream.producer;

import java.util.Map;

import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;
import org.nuxeo.lib.stream.pattern.producer.internals.ProducerRunner;

import io.dropwizard.metrics5.Timer;

public class MultiRepositoriesProducer<M extends DocumentMessage> extends ProducerRunner<M> {

	protected final Map<String, LogAppender<M>> appenders;

	public MultiRepositoriesProducer(ProducerFactory<M> factory, Map<String, LogAppender<M>> appenders,
			int producerId) {
		super(factory, null, producerId);
		this.appenders = appenders;
	}

	protected void producerLoop(ProducerIterator<M> producer) {
		M message;
		while (producer.hasNext()) {
			try (Timer.Context ignored = producerTimer.time()) {
				message = producer.next();
				setThreadName(message);
				counter++;
			}
			LogAppender<M> appender = getAppender(message);
			appender.append(producer.getPartition(message, appender.size()), message);
		}
	}

	protected LogAppender<M> getAppender(M message) {
		String state = null;
		if ("CustomerDocument".equals(message.getType()) ||"CustomerFolder".equals(message.getType())) {
			state = message.getParentPath().split("/")[1];
			state = USStateHelper.getStateCode(state);						
		} else if ("Folder".equals(message.getType()) ) {
			state = message.getName();
			state = USStateHelper.getStateCode(state);
		} 
		
		if (USStateHelper.isEastern(state)) {
			return appenders.get(USStateHelper.EAST);
		} else {
			return appenders.get(USStateHelper.WEST);
		}
	}

}
