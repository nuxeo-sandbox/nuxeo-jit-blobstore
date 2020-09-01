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

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

public class ImporterCompanionScroller implements Scroll {

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
			return scrollers.get(name);
		}
	}

	public static class ImporterCompanionScrollRequest implements ScrollRequest {

		public static final int DEFAULT_SCROLL_SIZE = 50;

		protected static final String SCROLL_TYPE = "importerCompanion";

		protected final String name;

		protected final int bucketSize;

		public ImporterCompanionScrollRequest(String name, int size) {
			this.name = name;
			this.bucketSize = size;
		}

		public ImporterCompanionScrollRequest(String name) {
			this(name, DEFAULT_SCROLL_SIZE);
		}

		public String getType() {
			return SCROLL_TYPE;
		}

		public String getName() {
			return name;
		}

		public int getSize() {
			return bucketSize;
		}

	}

	protected ConcurrentLinkedQueue<List<String>> buckets;

	protected ImporterCompanionScrollRequest request;

	protected List<String> buffer = new ArrayList<>();

	@Override
	public void init(ScrollRequest request, Map<String, String> options) {
		buckets = new ConcurrentLinkedQueue<>();
		this.request = (ImporterCompanionScrollRequest) request;
		register(this);
	}

	public void handle(List<String> commitedDocIds) {

		synchronized (buffer) {
			buffer.addAll(commitedDocIds);
			while (buffer.size() > request.getSize()) {

				List<String> newbucket = new ArrayList<>();
				newbucket.addAll(buffer.subList(0, request.getSize() - 1));
				buffer.removeAll(newbucket);
				buckets.add(newbucket);
			}
		}
	}

	public String getKey() {
		return this.request.name;
	}

	@Override
	public boolean hasNext() {
		return buckets.size() > 0;
	}

	@Override
	public List<String> next() {
		return buckets.poll();
	}

	@Override
	public void close() {
		buckets = null;
		unregister(this);
	}

}
