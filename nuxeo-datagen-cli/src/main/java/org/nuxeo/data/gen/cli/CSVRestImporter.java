package org.nuxeo.data.gen.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
		options.addOption("serverThreads", true, "Number of threads server side");
		options.addOption("o", "operation", true, "ConsumerProducers or AccountProducers");
		
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
		int nbServerThreads = Integer.parseInt(cmd.getOptionValue("serverThreads", "0"));
		
		String opId = cmd.getOptionValue('o', "ConsumerProducers");
		
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
					executor.submit(mkTask(page.toString(), nuxeoClient, split, logName, logSize, pageSize, nbServerThreads, opId));
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
				executor.submit(mkTask(page.toString(), nuxeoClient, split, logName, logSize, pageSize, nbServerThreads, opId));
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
		Long throughput= 0L;
		long pauseTimeS = 2;

		while (!finished) {

			long t1 = System.currentTimeMillis();
			int threads = executor.getActiveCount();

			long elapsed = (t1 - t0);
			throughput = Math.round(nbLines * 1000.0 / (elapsed ));

			System.out.println("   Throughput:" + throughput + " lines/s using " + threads + " threads");
			System.out.println(" Queue size: " + executor.getQueue().size());
			System.out.println(" Tasks count: " + executor.getTaskCount());

			try {
				finished = executor.awaitTermination(pauseTimeS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		int totalProducers=0;
		int totalMessages=0;
		int totalFailures=0;
		
		for (String stat : producersStats) {
			//System.out.println(ResponseHelper.formatAsString(stat));
			Map<String, Object> parsedStats = ResponseHelper.formatAsMap(stat);
			totalProducers+=(int) parsedStats.get("producers");
			totalMessages+=(int) parsedStats.get("messages");
			totalFailures+=(int) parsedStats.get("failures");
		}

		System.out.println(" ############################## ");
		System.out.println(" CSV stats:");
		System.out.println("    Throughput:" + throughput + " lines/s");
		System.out.println("    Total lines:" + nbLines);
		System.out.println(" Producers stats:");
		System.out.println("    total producers:" + totalProducers);
		System.out.println("    total messages:" + totalMessages);
		System.out.println("    total failures:" + totalFailures);

		long t1 = System.currentTimeMillis();	
		long elapsed = (t1 - t0);
		long msgThroughput = Math.round(totalMessages * 1000.0 / (elapsed));
		System.out.println("    Throughput:" + msgThroughput);
		
		if (nuxeoClient != null) {
			nuxeoClient.disconnect();
		}
	}

	protected static List<String> producersStats = new LinkedList<>();
	
	protected static Runnable mkTask(String csv, NuxeoClient client, boolean split, String logName, int logSize,
			int pageSize, int nbThreads, String opName) {
		return new Runnable() {

			@Override
			public void run() {
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("nbThreads", nbThreads);
				params.put("logName", logName);
				params.put("split", split);
				params.put("logSize", logSize);
				params.put("bufferSize", pageSize + 1);

				String opId = "StreamImporter.run" + opName;
				if (nbThreads>0) {
					opId = opId + "MT";
				}
						
				if (client == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					Operation op = client.operation(opId).parameters(params);
					Blob csvBlob = new StreamBlob(new ByteArrayInputStream(csv.getBytes()), "input.csv");
					op.input(csvBlob);
					String stats = (String) op.execute();
					synchronized (producersStats) {
						producersStats.add(stats);	
					}					
				}
			}

		};
	}

}
