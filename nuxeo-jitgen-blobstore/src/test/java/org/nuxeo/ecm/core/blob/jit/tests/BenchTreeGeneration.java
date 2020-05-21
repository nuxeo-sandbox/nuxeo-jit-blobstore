package org.nuxeo.ecm.core.blob.jit.tests;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.blob.jit.gen.DocInfo;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.NodeInfo;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.importer.stream.jit.HierarchyHelper;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, RepositoryElasticSearchFeature.class })

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
public class BenchTreeGeneration {

	protected static final int NB_CALLS = 100000;
	protected static final int NB_THREADS = 32;

	protected InMemoryBlobGenerator getGen() {
		return Framework.getService(InMemoryBlobGenerator.class);
	}

	@Test
	public void testGeneration() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NB_THREADS);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();

		int nbMonths = 60;

		long t0 = System.currentTimeMillis();
		SequenceGenerator sequenceGen = new SequenceGenerator(SequenceGenerator.DEFAULT_ACCOUNT_SEED, nbMonths);
		List<NodeInfo> hierarchy = HierarchyHelper.generateStateYearMonthHierarchy(nbMonths);

		final class Task implements Runnable {
			@Override
			public void run() {
				try {
					for (int i = 0; i < NB_CALLS; i++) {

						int nbMonths = 60;

						SequenceGenerator.Entry entry = sequenceGen.next();
						String parentPath;
						long currentAccountSeed = entry.getAccountKeyLong();
						long currentDataSeed = entry.getDataKey();
						int month = entry.getMonth();

						DocInfo docInfo = getGen().computeDocInfo("jit", currentAccountSeed, currentDataSeed, month);
						String state = docInfo.getMeta("STATE");
						int stateOffset = USStateHelper.getOffset(state);

						if (stateOffset < 0) {
							System.out.println("stateOffset=" + stateOffset);
							System.out.println("state=" + state);
							continue;
						}
						int nbYears = nbMonths / 12;
						int idx = stateOffset * (nbYears + nbMonths + 1) + nbYears + entry.getMonth() + 1;
						parentPath = hierarchy.get(idx).getPath();

						counter.incrementAndGet();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		for (int i = 0; i < NB_THREADS; i++) {
			executor.execute(new Task());
		}

		executor.shutdown();
		boolean finished = executor.awaitTermination(3 * 60, TimeUnit.SECONDS);
		if (!finished) {
			System.out.println("Timeout after " + counter.get() + " generations");
		}

		long t1 = System.currentTimeMillis();
		Double throughput = (counter.get() * 1.0 / (t1 - t0)) * 1000;
		System.out.println("Raw throughput:" + throughput);
	}

}
