package org.nuxeo.importer.stream.producer;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.importer.stream.jit.CustomerFolderMessageProducer;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;
import org.nuxeo.lib.stream.pattern.producer.internals.ProducerRunner;

import io.dropwizard.metrics5.Timer;

public class MultiRepositoriesProducer<M extends DocumentMessage> extends ProducerRunner<M> {

	private static final Log log = LogFactory.getLog(MultiRepositoriesProducer.class);

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
		if ("Customer".equals(message.getType()) ||"IDCard".equals(message.getType()) ||"Account".equals(message.getType()) || message.getType().startsWith("Correspondence")) {
			state = message.getParentPath().split("/")[1];
			state = USStateHelper.getStateCode(state);						
		} else if ("Domain".equals(message.getType()) ) {
			state = message.getName();
			state = USStateHelper.getStateCode(state);
		} else {
			String msg = "DocType " + message.getType() + " is not supported";
			RuntimeException e = new RuntimeException(msg);
			e.printStackTrace();
			log.error(e);
			throw e;
		}
		
		if (USStateHelper.isEastern(state)) {
			return appenders.get(USStateHelper.EAST);
		} else {
			return appenders.get(USStateHelper.WEST);
		}
	}

}
