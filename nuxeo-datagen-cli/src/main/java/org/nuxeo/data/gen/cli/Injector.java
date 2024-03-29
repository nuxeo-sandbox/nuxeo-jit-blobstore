/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 */

package org.nuxeo.data.gen.cli;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.data.gen.out.BlobWriter;
import org.nuxeo.data.gen.pdf.PDFFileGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;

public class Injector {

	public enum MODE {
		ID, METADATA, PDF;
	}

	protected int nbThreads = 10;
	protected int nbMonths = 48;
	protected int monthOffset=0;
	
	protected static final int BUFFER_SIZE = 10 * 1024;

	protected static final int MAX_PAUSE = 60 * 5;

	protected long total;
	protected int callsPerThreads = 5000;
	protected final PDFFileGenerator gen;

	protected BlobWriter writer;

	protected Logger importLogger;
	protected Logger metadataLogger;
	protected Logger cmdLogger;

	protected final MODE mode;

	protected RandomDataGenerator rndGen;

	protected final long seed;
	protected final long jump;
	

	public Injector(MODE mode, long seed, PDFFileGenerator gen, long jump, long total, int nbThreads, int nbMonths, int monthOffset,
			Logger importLogger, Logger metadataLogger, Logger cmdLogger) {
		this.mode = mode;
		this.gen = gen;
		this.total = total;
		this.nbThreads = nbThreads;
		this.callsPerThreads = Math.round(total / nbThreads) + 1;
		this.metadataLogger = metadataLogger;
		this.importLogger = importLogger;
		this.cmdLogger = cmdLogger;
		this.nbMonths = nbMonths;
		this.monthOffset=monthOffset;
		
		this.seed = seed;
		this.jump=jump;

		if (mode == MODE.PDF) {
			rndGen = new RandomDataGenerator(true, true);
			try {
				rndGen.init();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public BlobWriter getWriter() {
		return writer;
	}

	public void setWriter(BlobWriter writer) {
		this.writer = writer;
	}

	protected void log(String message) {
		if (cmdLogger != null) {
			cmdLogger.log(Level.INFO, message);
		} else {
			System.out.println(message);
		}
	}

	protected void collect(String id, String[] meta) {
		if (metadataLogger != null) {
			StringBuffer sb = new StringBuffer();
			sb.append(id);
			if (meta != null) {
				for (String key : meta) {
					sb.append(",");
					sb.append(key.trim());
				}
			}
			metadataLogger.log(Level.DEBUG, sb.toString());
		}
	}

	protected void collect(StatementMeta meta) {
		if (metadataLogger != null) {
			StringBuffer sb = new StringBuffer();
			sb.append(meta.getDigest());
			sb.append(",");
			sb.append(meta.getFileName());
			sb.append(",");
			sb.append(meta.getFileSize());
			for (String key : meta.getKeys()) {
				sb.append(",");
				if (key != null) {
					sb.append(key.trim());
				} else {
					sb.append("null");
				}
			}
			metadataLogger.log(Level.DEBUG, sb.toString());
		}
	}

	protected void logDuration(Duration d, String message) {
		StringBuilder sb = new StringBuilder(message);
		if (d.toDaysPart() > 0)
			sb.append(d.toDaysPart()).append(" days,");
		if (d.toHoursPart() > 0)
			sb.append(d.toHoursPart()).append(" h,");
		if (d.toMinutesPart() > 0)
			sb.append(d.toMinutesPart()).append(" m,");
		sb.append(d.toSecondsPart()).append(" s");
		log(sb.toString());
	}

	public int run() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();

		// force init
		SequenceGenerator.getDataGenerator();
		
		if (mode==MODE.ID) {
			collect("ID", null);
		} else if (mode==MODE.METADATA) {
			collect("ID", new String[]{"CustomerName","AddressStreet","AddressCity","AddressState","Date","AccountNumber","BlobKey"});
		} else if (mode == MODE.PDF) {
			collect("digest", new String[]{"filename","fileSize","CustomerName","AddressStreet","AddressCity","AddressState","Date","AccountNumber","BlobKey"});
		}

		log("----------------------------------------------------------");

		LoggerHelper.silencePDFBox();
		
		long t0 = System.currentTimeMillis();

		SequenceGenerator sg = new SequenceGenerator(seed, nbMonths);
		sg.setMonthOffset(monthOffset);
		
		if (jump>0) {
			importLogger.log(Level.INFO, "Skipping sequence generator until " + jump);
			sg.skip(jump);
			importLogger.log(Level.INFO, "Skip completed - starting import");
		}

		importLogger.log(Level.INFO, "Initialized data generator with seed %d on %d months and offse %d", seed, nbMonths, monthOffset);
		
		final class Task implements Runnable {

			@Override
			public void run() {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE);

				for (int i = 0; i < callsPerThreads; i++) {
					try {
						buffer.reset();
						SequenceGenerator.Entry entry = sg.next();

						if (mode == MODE.ID) {
							String id = entry.getAccountID();
							collect(id, null);
						} else if (mode == MODE.METADATA) {
							String id = entry.getAccountID();
							collect(id, entry.getMetaData());
						} else if (mode == MODE.PDF) {
							StatementMeta smeta = null;
							if (gen.getType().equalsIgnoreCase("IdCard")) {
								sg.setAccountsRange(0);
								String[] meta = entry.getMetaData();
								smeta = gen.generate(buffer, meta);
							} else if (gen.getType().equalsIgnoreCase("NewAccountLetter")) {
								//sg.setAccountsRange(0);
								String[] meta = entry.getMetaData();
								smeta = gen.generate(buffer, meta);
							} else {
								String[] meta = rndGen.generate(entry.getAccountKeyLong(), entry.getDataKey(),
										entry.getMonth());
								smeta = gen.generate(buffer, meta);
							}

							if (writer != null) {
								writer.write(buffer.toByteArray(), smeta);
							}
							collect(smeta);
						}
						counter.incrementAndGet();
					} catch (Exception e) {
						importLogger.log(Level.ERROR, "Unable to write", e);					
						e.printStackTrace();
						return;
					}
				}
			}
		}

		for (int i = 0; i < nbThreads; i++) {
			log("Starting thread " + i);
			executor.execute(new Task());
		}

		executor.shutdown();
		boolean finished = false;
		Long throughput;
		long pauseTimeS = 2;

		while (!finished) {

			long t1 = System.currentTimeMillis();
			long count = counter.get();
			int threads = executor.getActiveCount();

			long elapsed = (t1 - t0);
			throughput = Math.round(counter.get() * 1.0 / (elapsed / 1000));
			long percentCompleted = Math.round((count * 100.0) / total);

			log(String.format("%02d %% - %d / %d", percentCompleted, count, total));

			// log(percentCompleted + "% - " + count + "/" + total );
			log("   Throughput:" + throughput + " d/s using " + threads + " threads");

			if (throughput.intValue() > 0) {
				Duration d = Duration.ofSeconds((total - count) / throughput.intValue());
				logDuration(d, "   Projected remaining time: ");

				pauseTimeS = 1 + Math.round(d.toSeconds() / 100);
				if (pauseTimeS > MAX_PAUSE) {
					pauseTimeS = MAX_PAUSE;
				}
			}

			finished = executor.awaitTermination(pauseTimeS, TimeUnit.SECONDS);
		}

		
		if (writer != null) {
			log("Flushing ...");
			writer.flush();
			log("Flush done.");
		}

		long t1 = System.currentTimeMillis();
		throughput = Math.round(counter.get() * 1.0 / ((t1 - t0) / 1000));

		log("----------------------------------------------------------");

		log(counter.get() + " files generated.");

		Duration d = Duration.ofSeconds(((t1 - t0) / 1000));
		logDuration(d, "  Execution time: ");

		log("  Average throughput:" + throughput.intValue() + " docs/s");

		printProjections(throughput.intValue());

		return throughput.intValue();
	}

	protected void printProjections(int throughput) {
		
		long[] targets = new long[] {100000000, 1000000000, 10000000000L };
		String[] labels = new String[] {"100M", "1B", "10B" };
		
		log("#### Projected generation time:");
		for (int i= 0 ; i < targets.length; i++) {
			if (throughput > 0) {
				Duration d = Duration.ofSeconds(targets[i] / throughput);
				log("     - for " + labels[i] + " files: " + d.toDaysPart() + " day(s) and " + d.toHoursPart()
						+ " hour(s)]");
			}
		}
	}
}
