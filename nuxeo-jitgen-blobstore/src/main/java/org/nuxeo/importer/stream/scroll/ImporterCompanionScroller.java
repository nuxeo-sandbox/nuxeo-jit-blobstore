/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.scroll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;

public class ImporterCompanionScroller implements Scroll {

	private static final Log log = LogFactory.getLog(ImporterCompanionScroller.class);

	protected static Map<String, ImporterCompanionScroller> scrollers = new HashMap<>();

	protected static void register(ImporterCompanionScroller scroller) {
		synchronized (scrollers) {
			scrollers.put(scroller.getKey(), scroller);
		}
	}

	protected static void unregister(ImporterCompanionScroller scroller) {
		synchronized (scrollers) {
			scrollers.remove(scroller.getKey());
		}
	}

	public static ImporterCompanionScroller getInstance(String name) {
		synchronized (scrollers) {
			if (scrollers.containsKey(name)) {
				return scrollers.get(name);
			} else {
				return null;
			}
		}
	}

	public static boolean kill(String name) {
		synchronized (scrollers) {
			if (scrollers.containsKey(name)) {
				scrollers.get(name).finished = true;
				return true;
			} else {
				return false;
			}
		}
	}

	protected ConcurrentLinkedQueue<List<String>> buckets;

	protected GenericScrollRequest request;

	protected List<String> buffer = new ArrayList<>();

	protected String logName;

	protected boolean finished;

	public static final String FINISHED_TOKEN = "<--END-->";

	protected static final int TIMEOUTS = 5 * 60;

	protected int inCount=0;

	protected int outCount=0;
	
	@Override
	public void init(ScrollRequest request, Map<String, String> options) {
		buckets = new ConcurrentLinkedQueue<>();
		this.request = (GenericScrollRequest) request;
		logName = this.request.getQuery();
		finished = false;
		register(this);
	}

	public void handle(List<String> commitedDocIds) {

		synchronized (buffer) {
			inCount+=commitedDocIds.size();
			buffer.addAll(commitedDocIds);
			while (buffer.size() >= request.getSize()) {

				List<String> newbucket = new ArrayList<>();
				newbucket.addAll(buffer.subList(0, request.getSize() - 1));
				buffer.removeAll(newbucket);
				// send FINISHED_TOKEN to tell the stream is over
				if (newbucket.contains(FINISHED_TOKEN)) {
					finished = true;
					newbucket.remove(FINISHED_TOKEN);
				}
				buckets.add(newbucket);
			}
		}
	}

	public String getKey() {
		return this.logName;
	}
	
	protected boolean waitForNewMessagesOrFinished() {

		long t0 = System.currentTimeMillis();
		// wait for more data
		while ((buckets.size() == 0) && (System.currentTimeMillis() - t0 < TIMEOUTS * 1000) && ! finished) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (buckets.size() > 0) {
			return true;
		} else if (buckets.size() == 0 && finished) {		
			return flushIfNeededAndFinish();
		} else {
			log.warn("Scroller will  exit with timeout");					
			return flushIfNeededAndFinish();
		}
	}
	
	protected boolean flushIfNeededAndFinish() {
		if (buffer.size()>0) {
			List<String> partialbucket = new ArrayList<>();
			partialbucket.addAll(buffer);
			buckets.add(partialbucket);
			buffer.clear();
			finished=true;
			return true;		
		}
		return false;	
	}
	
	
	@Override
	public boolean hasNext() {
		if (buckets.size() > 0) {
			return true;
		} else {
			if (finished) {
				return flushIfNeededAndFinish();
			} else {
				return waitForNewMessagesOrFinished();
			}
		}
	}

	@Override
	public List<String> next() {
		List<String> ids = buckets.poll();
		if (ids!=null) {
			outCount+=ids.size();
		}
		return ids;
	}

	@Override
	public void close() {
		buckets = null;
		unregister(this);
		String msg = String.format("Scroller closed after having received %s and produced %s ids", inCount, outCount);		
		log.info(msg);
	}

}
