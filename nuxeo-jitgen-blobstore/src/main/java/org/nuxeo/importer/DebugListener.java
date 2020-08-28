package org.nuxeo.importer;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class DebugListener implements EventListener {

	private static final Logger log = LogManager.getLogger(DebugListener.class);

	public static class LogOutputStream extends OutputStream {
		private StringBuffer mem;

		public LogOutputStream() {
			mem = new StringBuffer();
		}

		@Override
		public void write(final int b) {
			if ((char) b == '\n') {
				flush();
				return;
			}
			mem = mem.append((char) b);
		}

		@Override
		public void flush() {
			log.error(mem.toString());
			mem = new StringBuffer();
		}
	}

	@Override
	public void handleEvent(Event event) {

		DocumentEventContext ctx = (DocumentEventContext) event.getContext();

		DocumentModel doc = ctx.getSourceDocument();
		log.error("setACP was called on " + doc.getRepositoryName() + ":" + doc.getPathAsString());

		if ("/".equals(doc.getPathAsString())) {
			log.error("setACP was called on ROOT for repository " + doc.getRepositoryName());
			PrintStream ps = new PrintStream(new LogOutputStream());
			new Exception("setACPCalled").printStackTrace(ps);
			ps.flush();
			//Thread.currentThread().dumpStack();
		}
	}

}
