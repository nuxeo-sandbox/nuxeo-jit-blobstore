package org.nuxeo.data.gen.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.NuxeoVersion;
import org.nuxeo.client.objects.Operation;
import org.nuxeo.client.objects.blob.Blob;
import org.nuxeo.client.objects.blob.StreamBlob;

public class CSVRestImporter {

	protected static final int PAGE_SIZE = 1000;

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption("c", "config", true, "Configuration file - default is nuxeo.properties");
		options.addOption("t", "threads", true, "Number of threads");
		options.addOption("h", "help", false, "Help");
		options.addOption("f", "file", true, "location of CSV file to import");
		options.addOption("l", "logName", true, "name (or prefix) of the stream to use");
		options.addOption("m", "multiRepo", false, "Define if multi-repositories is used");
		options.addOption("p", "logSize", true, "Number og partitions using in the stream");
		options.addOption("b", "bucketSize", true, "Number of lines per page");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			return;
		}

		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("RestCLI", options);
			return;
		}

		Properties nuxeoConfig = new Properties();
		String config = cmd.getOptionValue('c', "nuxeo.properties");
		try {
			File configFile = new File(config);
			System.out.println("Using config " + configFile.getAbsolutePath());
			nuxeoConfig.load(new FileReader(configFile));
		} catch (Exception e) {
			System.err.println("Unable to read configuration file " + e.getMessage());
			return;
		}

		NuxeoClient nuxeoClient = NuxeoClientHelper.createClient(nuxeoConfig);
		if (nuxeoClient != null) {
			NuxeoVersion version = nuxeoClient.getServerVersion();
			System.out.println("Connected to Nuxeo Server " + version.toString());
		}

		int nbThreads = Integer.parseInt(cmd.getOptionValue('t', "10"));
		int pageSize = Integer.parseInt(cmd.getOptionValue('b', "" + PAGE_SIZE));
		int logSize = Integer.parseInt(cmd.getOptionValue('p', "8"));

		String csvFileName = cmd.getOptionValue('f', null);
		File csvFile = null;
		if (csvFileName != null) {
			csvFile = new File(csvFileName);
		} else {
			System.err.println("No imput file");
			return;
		}

		String logName = cmd.getOptionValue('l', "import/Customers");
		boolean split = false;
		if (cmd.hasOption("m")) {
			split = true;
		}

		long t0 = System.currentTimeMillis();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		executor.prestartAllCoreThreads();

		System.out.println("Started ThreadPool with " + nbThreads + " threads");
		FileInputStream inputStream = null;
		Scanner sc = null;

		StringBuffer page = new StringBuffer();
		long nbLines = 0;
		int batches = 0;
		try {
			inputStream = new FileInputStream(csvFile);
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				nbLines++;
				page.append(line).append("\n");
				if (nbLines % pageSize == 0) {
					executor.submit(mkTask(page.toString(), nuxeoClient, split, logName, logSize, pageSize));
					batches++;
					System.out.println("Send batch " + batches);
					page = new StringBuffer();

					while (executor.getQueue().size() > 25) {
						Thread.sleep(1000);
						System.out.print(".");
					}
					System.out.print("\n");
				}
			}
			if (page.length() > 0) {
				executor.submit(mkTask(page.toString(), nuxeoClient, split, logName, logSize, pageSize));
				batches++;
			}
			if (sc.ioException() != null) {
				throw sc.ioException();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (sc != null) {
				sc.close();
			}
		}

		executor.shutdown();
		boolean finished = false;
		Long throughput;
		long pauseTimeS = 2;

		while (!finished) {

			long t1 = System.currentTimeMillis();
			int threads = executor.getActiveCount();

			long elapsed = (t1 - t0);
			throughput = Math.round(nbLines * 1.0 / (elapsed / 1000));

			System.out.println("   Throughput:" + throughput + " d/s using " + threads + " threads");
			System.out.println(" Queue size: " + executor.getQueue().size());
			System.out.println(" Tasks count: " + executor.getTaskCount());

			try {
				finished = executor.awaitTermination(pauseTimeS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (nuxeoClient != null) {
			nuxeoClient.disconnect();
		}
	}

	protected static Runnable mkTask(String csv, NuxeoClient client, boolean split, String logName, int logSize,
			int pageSize) {
		return new Runnable() {

			@Override
			public void run() {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("nbThreads", 1);
				params.put("logName", logName);
				params.put("split", split);
				params.put("logSize", logSize);
				params.put("bufferSize", pageSize + 1);

				if (client == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					Operation op = client.operation("StreamImporter.runConsumerProducers").parameters(params);
					Blob csvBlob = new StreamBlob(new ByteArrayInputStream(csv.getBytes()), "input.csv");
					op.input(csvBlob);
					op.voidOperation(true).execute();
				}
			}

		};
	}

}
