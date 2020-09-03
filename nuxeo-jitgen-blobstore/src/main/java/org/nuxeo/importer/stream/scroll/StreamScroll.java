package org.nuxeo.importer.stream.scroll;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

public class StreamScroll implements Scroll {

	protected GenericScrollRequest request;
	
	LogTailer<Message> tailer;
	
	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> next() {
		
		//tailer
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(ScrollRequest request, Map<String, String> options) {

		this.request = (GenericScrollRequest)request; 
		LogManager manager = Framework.getService(StreamService.class).getLogManager();

		Name group = Name.ofUrn("");
		Name log = Name.ofUrn("");
		
		tailer = manager.createTailer(group, log);

	}

	@Override
	public void close() {
		tailer.commit();
		tailer.close();
	}

}
